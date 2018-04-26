import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TestC {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*final long timestamp = new Date().getTime();

		// with java.util.Date/Calendar api
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);
		// here's how to get the minutes
		final int minutes = cal.get(Calendar.MINUTE);
		// and here's how to get the String representation
		final String timeString =
		    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
		System.out.println(minutes);
		System.out.println(timeString);*/
		TestC testC = new TestC();
		
		/*for(String[] strings : testC.enquiryFiles()) {
			System.out.println(strings[0]);
			System.out.println(strings[1]);
		}*/
		
		/*byte[] bytes = new byte[2048];
		Arrays.fill(bytes, (byte)0);
		String str = new String(bytes);
		System.out.println(str);
		byte[] bs = str.getBytes();
		System.out.println(bs.length == bytes.length);
		if(bytes.length == bs.length) {
			for(int i = 0; i < bytes.length; i++) {
				if(bs[i] != bytes[i]) {
					System.out.println("a byte different");
				}
			}
			System.out.println("same");
		}else {
			System.out.println("length is not same");
		}*/
		
		/*Map<String, Integer> map = new HashMap<>();
		int i = map.get("asd");*/
		Set<Integer> set = new HashSet<>(0);
		set.add(4);
		set.add(5);
		set.add(1);
		set.add(2);
		set.add(3);
		set.add(6);
		set.add(7);
		set.add(8);
		String pre = null;
		for(int i = 0; i < 100; i++) {
			StringBuilder sb = new StringBuilder();
			System.out.println(set);
			for(Integer num : set) {
				sb.append(num);
			}
			if(pre != null && !pre.equals(sb.toString())) {
				System.out.println("Non Order");
			}
			pre = sb.toString();
		}
		System.out.println("Order");
		
	}
	
	private synchronized List<String[]> enquiryFiles() {
		String FILEPREFIX = ".//files" + 1 + "//";
		List<String[]> list = new ArrayList<>();
		File folder = new File(FILEPREFIX);
		File[] files = folder.listFiles();
		Calendar cal = Calendar.getInstance();
		for (File file : files) {
			cal.setTimeInMillis(file.lastModified());
			String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
			list.add(new String [] {file.getName(), timeString});
		}
		return list;
	}

}
