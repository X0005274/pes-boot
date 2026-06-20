package com.playtogether.pes.lot.api;

/**
 * LOT 도메인 TIBCO RV Subject 상수.
 * Reply 는 RV _INBOX 로 자동 생성되므로 정의하지 않는다.
 */
public final class LotSubjects {

    private LotSubjects() {
    }

    public static final String UI_REQUEST = "PES.UI.LOT.REQUEST";
    public static final String UI_EVENT = "PES.UI.LOT.EVENT";
    public static final String BIZ_EVENT = "PES.BIZ.LOT.EVENT";
}
