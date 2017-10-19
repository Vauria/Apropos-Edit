package com.loverslab.apropos.edit;

import java.util.Arrays;

public enum Position {
	FF_Anal_Change, FF_Anal, FF_Oral_Change, FF_Oral, FF_Vaginal_Change, FF_Vaginal, Anal_Change, Anal, BoobJob_Change, BoobJob, FFM, Fisting_Change, Fisting, FootJob_Change, FootJob, GangBang_Change, GangBang, HandJob_Change, HandJob, Lesbian_Change, Lesbian, MMF, MMMF, MMMMF, MMMMMF, MMMMMMF, Masturbation, Oral_Change, Oral, Vaginal_Change, Vaginal, VirginityLost_Anal, VirginityLost_Oral, VirginityLost_Vaginal, WearTearIncreased_Anal, WearTearIncreased_Oral, WearTearIncreased_Vaginal, WearTearReduced_Anal, WearTearReduced_Oral, WearTearReduced_Vaginal, Unique;
	
	public static Position lookup( String str ) {
		return Arrays.stream( Position.values() ).filter( e -> e.name().equalsIgnoreCase( str ) ).findAny().orElse( null );
	}
}

/*
 * Anal, Anal_Change, BoobJob, BoobJob_Change, Boobjob, Boobjob_Change, FF_Anal, FF_Anal_Change, FF_Oral, FF_Oral_Change, FF_Vaginal,
 * FF_Vaginal_Change, FFM,
 * Fisting, Fisting_Change, FootJob, FootJob_Change, Footjob, Footjob_Change, GangBang, GangBang_Change, HandJob, HandJob_Change, Handjob,
 * Handjob_Change, Lesbian, Lesbian_Change, MMF,
 * Masturbation, Oral, Oral_Change, Vaginal, Vaginal_Change, VirginityLost_Anal, VirginityLost_Oral, VirginityLost_Vaginal,
 * WearTearIncreased_Anal, WearTearIncreased_Oral, WearTearIncreased_Vaginal, WearTearReduced_Anal, WearTearReduced_Oral,
 * WearTearReduced_Vaginal, Unique;
 */