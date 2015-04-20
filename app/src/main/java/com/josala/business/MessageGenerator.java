package com.josala.business;

import com.josala.ibeacon.IBeacon;

public class MessageGenerator {

    public MessageGenerator() {
    }

    public static String generateMessage(IBeacon b){
		String message = "you have entered business";
		
		if (b.getMajor() == IBeacon.MAJOR_BUILDING && b.getMinor() == IBeacon.MINOR_ZONE1){
			message = "zone 1";
		}
		else if (b.getMajor() == IBeacon.MAJOR_BUILDING && b.getMinor() == IBeacon.MINOR_ZONE2){
			message = "zone 2";
		}
		return message;
	}

}
