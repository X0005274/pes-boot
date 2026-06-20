package com.playtogether.pes.durable.api;

/**
 * DURABLE 도메인 TIBCO RV Subject 상수. Reply 는 RV _INBOX 자동 생성.
 */
public final class DurableSubjects {

    private DurableSubjects() {
    }

    public static final String UI_REQUEST = "PES.UI.DURABLE.REQUEST";
    public static final String UI_EVENT = "PES.UI.DURABLE.EVENT";
    public static final String BIZ_EVENT = "PES.BIZ.DURABLE.EVENT";
}
