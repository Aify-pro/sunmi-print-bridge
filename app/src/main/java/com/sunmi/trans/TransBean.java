package com.sunmi.trans;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Stub vide — voir TransBean.aidl. Uniquement là pour que
 * IWoyouService.commitPrint() compile et garde sa position dans
 * l'interface AIDL ; jamais instancié par PrintActivity.
 */
public class TransBean implements Parcelable {

    public TransBean() {}

    protected TransBean(Parcel in) {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {}

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TransBean> CREATOR = new Creator<TransBean>() {
        @Override
        public TransBean createFromParcel(Parcel in) {
            return new TransBean(in);
        }

        @Override
        public TransBean[] newArray(int size) {
            return new TransBean[size];
        }
    };
}
