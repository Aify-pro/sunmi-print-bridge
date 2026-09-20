package com.sunmi.trans;

/**
 * Déclaration "parcelable existant" pour com.sunmi.trans.TransBean — seule
 * la signature d'IWoyouService.commitPrint() en a besoin pour rester à la
 * bonne position dans l'interface (voir IWoyouService.aidl). PrintActivity
 * n'appelle jamais commitPrint() : le contenu réel du type ne nous
 * concerne pas, seul son statut de Parcelable compile-t-il correctement
 * l'AIDL. Implémentation Java minimale en vis-à-vis dans
 * java/com/sunmi/trans/TransBean.java.
 */
parcelable TransBean;
