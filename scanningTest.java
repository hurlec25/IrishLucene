package Lucene;
import java.io.File;
import java.util.Scanner;

public class scanningTest {
	public static void main(String[] args){
		try {
			Scanner sc=new Scanner(new File("/home/hurlec25/IrishLucene-master/TestHighLight/a2.txt"));
			String a="";

			while(sc.hasNext()){
				a=a+" "+sc.next();
			}
			System.out.println(a);
			sc.close();
		}
		
		catch(Exception e) {
	        e.printStackTrace();
	    }
	}
}
