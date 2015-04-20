package com.josala.ibeacon;

public class IBeacon {

    // Configured values
    final public static int MAJOR_BUILDING = 65432;
    final public static int MINOR_ZONE1 = 99;
    final public static int MINOR_ZONE2 = 88;

    // Generic values
	final private static int START_UUID_BYTE = 9;
	final private static int START_MAJOR_BYTE = 25;
	final private static int LAST_MAJOR_BYTE = 26;
	final private static int START_MINOR_BYTE = 27;
	final private static int LAST_MINOR_BYTE = 28;
    final private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    
    /**
     * A 16 byte UUID that typically represents the company owning a number of iBeacons
     * Example: E2C56DB5-DFFB-48D2-B060-D0F5A71096E0 
     */
    protected String proximityUuid;
    /**
     * A 16 bit integer typically used to represent a group of iBeacons
     */
    protected int major;
    /**
     * A 16 bit integer that identifies a specific iBeacon within a group 
     */
    protected int minor;
	public String getProximityUuid() {
		return proximityUuid;
	}
	public void setProximityUuid(String proximityUuid) {
		this.proximityUuid = proximityUuid;
	}
	public int getMajor() {
		return major;
	}
	public void setMajor(int major) {
		this.major = major;
	}
	public int getMinor() {
		return minor;
	}
	public void setMinor(int minor) {
		this.minor = minor;
	}
    
	public static int getMajorFromScan (byte[] input){
		int result = 0;
		
        result = (input[START_MAJOR_BYTE] & 0xff) * 0x100 + (input[LAST_MAJOR_BYTE] & 0xff);
		return result;
	};

	public static int getMinorFromScan (byte[] input){
		int result = 0;
		
        result = (input[START_MINOR_BYTE] & 0xff) * 0x100 + (input[LAST_MINOR_BYTE] & 0xff);
		return result;
	};
	
	public static String getUUIDFromScan (byte[] input){
		String result = null;
		
        byte[] uuidBytes = new byte[16];
        System.arraycopy(input, START_UUID_BYTE, uuidBytes, 0, 16); 
		
        String hexString = bytesToHex(uuidBytes);
        StringBuilder sb = new StringBuilder();
        sb.append(hexString.substring(0,8));
        sb.append("-");
        sb.append(hexString.substring(8,12));
        sb.append("-");
        sb.append(hexString.substring(12,16));
        sb.append("-");
        sb.append(hexString.substring(16,20));
        sb.append("-");
        sb.append(hexString.substring(20,32));
        result = sb.toString();

		return result;
	}

	private static String bytesToHex(byte[] bytes) {
		
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
	};

}
