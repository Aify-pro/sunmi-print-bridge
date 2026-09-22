package com.kiosk.printbridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Écran d'accueil de la tablette : plein écran sur https://seritex.vercel.app,
 * sans barre d'adresse, sans sortie vers le launcher Android normal. Reçoit le
 * démarrage automatique via son intent-filter HOME/DEFAULT (voir
 * AndroidManifest.xml) — c'est en devenant l'app d'accueil par défaut (choix
 * « Toujours » dans le sélecteur qui s'affiche à l'appui sur Accueil) qu'elle
 * s'ouvre à chaque redémarrage de la tablette, sans "boot receiver".
 *
 * Verrouillage volontairement strict (demande explicite) : bouton Retour
 * ignoré une fois qu'on est sur la page racine, et startLockTask() épingle la
 * tâche pour masquer Accueil/Récents. Sans réinscrire la tablette comme
 * "Device Owner" (ce qui demanderait une réinitialisation d'usine complète),
 * Android garde une échappatoire système standard : appui long simultané sur
 * Retour + Récents. C'est documenté ici plutôt que caché.
 *
 * La barre de statut du haut reste volontairement visible et déroulable
 * (accès Wi-Fi depuis les réglages rapides, demande explicite) : seule la
 * barre de navigation du bas est masquée. startLockTask() empêche déjà de
 * quitter l'écran vers Accueil/Récents depuis ce menu déroulant.
 *
 * Sur la Sunmi V2, un mécanisme codé en dur dans system_server relance de
 * force woyou.launcher (le launcher d'usine) environ 15 s après le
 * démarrage — logs : "PMV2Utils: CUSTOM_LAUNCHER: com.woyou.launcher" suivi
 * d'un START explicite depuis l'UID système. Rien d'installable ou de
 * désactivable ne permet de l'empêcher sans root. watchdog() contre-attaque
 * en relançant KioskActivity toutes les WATCHDOG_INTERVAL_MS : sans effet
 * quand on est déjà au premier plan (singleTask → onNewIntent, pas de
 * rechargement), et reprend la main sinon.
 */
public class KioskActivity extends Activity {

    private static final String TAG = "SunmiPrintBridge";
    private static final String SERITEX_URL = "https://seritex.vercel.app";
    private static final long RETRY_DELAY_MS = 5000;
    private static final long WATCHDOG_INTERVAL_MS = 2500;

    private WebView webView;
    private final Handler watchdogHandler = new Handler();
    private final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            Intent self = new Intent(KioskActivity.this, KioskActivity.class);
            self.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(self);
            watchdogHandler.postDelayed(this, WATCHDOG_INTERVAL_MS);
        }
    };

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemBars();

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    return false; // reste dans cette WebView, pas de navigateur externe
                }
                // Le bouton d'impression de Seritex fait
                // window.location.href = "sunmiprint://…" : une WebView n'ouvre
                // pas seule ce schéma, on le route vers PrintActivity.
                if ("sunmiprint".equals(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception e) {
                        Log.e(TAG, "Impossible d'ouvrir " + scheme + "://", e);
                    }
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Réseau coupé : on réessaie tant qu'on n'est pas revenu, sinon la
                // tablette (verrouillée) resterait bloquée sur une page d'erreur.
                if (request.isForMainFrame()) {
                    view.postDelayed(() -> view.loadUrl(SERITEX_URL), RETRY_DELAY_MS);
                }
            }
        });
        webView.loadUrl(SERITEX_URL);
        setContentView(webView);

        watchdogHandler.postDelayed(watchdog, WATCHDOG_INTERVAL_MS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        try {
            startLockTask();
        } catch (Exception ignored) {
            // Pas grave si le pinning refuse (ex. déjà épinglé) : l'écran
            // d'accueil reste quand même actif.
        }
    }

    /** Masque uniquement la barre de navigation du bas ; la barre de statut du
     * haut reste visible et déroulable (accès Wi-Fi). */
    private void hideSystemBars() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    @Override
    public void onBackPressed() {
        // Navigue en arrière dans l'appli web si possible ; sinon on ignore —
        // pas de retour au launcher Android.
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    @Override
    protected void onDestroy() {
        watchdogHandler.removeCallbacks(watchdog);
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
