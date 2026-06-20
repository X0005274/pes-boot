package com.playtogether.pes.wf.api;

/**
 * WF 도메인 TIBCO RV Subject 상수. Reply 는 RV _INBOX 자동 생성.
 */
public final class WfSubjects {

    private WfSubjects() {
    }

    public static final String UI_REQUEST = "PES.UI.WF.REQUEST";
    public static final String UI_EVENT = "PES.UI.WF.EVENT";
    public static final String BIZ_EVENT = "PES.BIZ.WF.EVENT";
}
