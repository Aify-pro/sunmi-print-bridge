/**
* JIUI V1 打印服务
* AIDL Version: 2.1
*
* Interface officielle Sunmi (woyou.aidlservice.jiuiv5), copiée telle
* quelle depuis leur SDK public. L'ordre et les signatures exacts des
* méthodes comptent : AIDL distribue les appels par position déclarée dans
* ce fichier, pas par nom — un fichier tronqué ou réordonné fait exécuter
* la MAUVAISE méthode côté service réel (constaté : nos anciens appels
* setFontSize() atterrissaient sur printerSelfChecking() et imprimaient une
* page de test à chaque impression). Ne pas retirer ni réordonner une
* méthode ici, même non utilisée par PrintActivity — ça décale tout ce qui
* suit.
*/

package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.ICallback;
import android.graphics.Bitmap;
import com.sunmi.trans.TransBean;

interface IWoyouService
{
	void updateFirmware();

	int getFirmwareStatus();

	String getServiceVersion();

	void printerInit(in ICallback callback);

	void printerSelfChecking(in ICallback callback);

	String getPrinterSerialNo();

	String getPrinterVersion();

	String getPrinterModal();

	void getPrintedLength(in ICallback callback);

	void lineWrap(int n, in ICallback callback);

	void sendRAWData(in byte[] data, in ICallback callback);

	void setAlignment(int alignment, in ICallback callback);

	void setFontName(String typeface, in ICallback callback);

	void setFontSize(float fontsize, in ICallback callback);

	void printText(String text, in ICallback callback);

	void printTextWithFont(String text, String typeface, float fontsize, in ICallback callback);

	void printColumnsText(in String[] colsTextArr, in int[] colsWidthArr, in int[] colsAlign, in ICallback callback);

	void printBitmap(in Bitmap bitmap, in ICallback callback);

	void printBarCode(String data, int symbology, int height, int width, int textposition,  in ICallback callback);

	void printQRCode(String data, int modulesize, int errorlevel, in ICallback callback);

	void printOriginalText(String text, in ICallback callback);

	void commitPrint(in TransBean[] transbean, in ICallback callback);

	void commitPrinterBuffer();

	void enterPrinterBuffer(in boolean clean);

	void exitPrinterBuffer(in boolean commit);

	void printColumnsString(in String[] colsTextArr, in int[] colsWidthArr, in int[] colsAlign, in ICallback callback);

	void printBitmapCustom(in Bitmap bitmap, in int type, in ICallback callback);
}
