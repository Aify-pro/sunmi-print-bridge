package com.kiosk.printbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Lance KioskActivity juste après le démarrage de la tablette, par-dessus le
 * launcher Sunmi (woyou.launcher). Nécessaire car cette ROM ignore la
 * préférence Android standard pour l'app d'accueil par défaut (confirmé :
 * `cmd package set-home-activity` s'enregistre bien dans les préférences du
 * système — visible dans `dumpsys package` — mais au démarrage comme lors
 * d'un nouvel intent HOME envoyé manuellement, c'est toujours woyou.launcher
 * qui est relancé). Ce receiver contourne ce verrou constructeur en ouvrant
 * directement notre écran, sans dépendre de cette résolution.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        Intent kiosk = new Intent(context, KioskActivity.class);
        kiosk.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(kiosk);
    }
}
