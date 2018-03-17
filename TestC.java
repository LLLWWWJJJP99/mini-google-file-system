import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
		
		for(String[] strings : testC.enquiryFiles()) {
			System.out.println(strings[0]);
			System.out.println(strings[1]);
		}
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
