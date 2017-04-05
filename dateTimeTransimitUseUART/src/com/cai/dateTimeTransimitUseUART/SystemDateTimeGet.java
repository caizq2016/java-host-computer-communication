package com.cai.dateTimeTransimitUseUART;
import java.util.Calendar;
import java.util.Date;
/*
 * 获取系统当前时间
 */
public class SystemDateTimeGet
{
	//类变量 时间的校验和
	static int  timeCheckSum=0;

	public static String getCurrentDateTime()
	{
		//单例模式
		 Calendar calendar=Calendar.getInstance();
		 int year = calendar.get(Calendar.YEAR);//获取年份  
         int month=calendar.get(Calendar.MONTH);//获取月份   
         int day=calendar.get(Calendar.DATE);//获取日期  
         int minute=calendar.get(Calendar.MINUTE);//分   
         int hour=calendar.get(Calendar.HOUR);//小时   
         int second=calendar.get(Calendar.SECOND);//秒  
         if(hour>=12)
         {
        	 hour=hour+12;
         }
         String curerentDateTime= year + " " + (month + 1 )+ " " + day + " "+ hour + " " + minute + " " + second + " ";
         timeCheckSum=year+(month+1)+day+(hour+12)+minute+second;
         return curerentDateTime;  
	}
	/*
	 * 将以上时间字符串进行隔开用byte[]保存
	 */
	public static byte[] dateTimeBytesGet(String currenDateTime)
	{
		//对当前时间参数进行格式判断
		//对格式进行判断
		int rawDataSize=6;
		byte[] dateTimeBytes=new byte[rawDataSize+1];
		String[] currentDateTimeSplit=currenDateTime.split(" ");
		if(currentDateTimeSplit.length==rawDataSize)
		{
			//时间数据格式正确
			//eg 2016 12 23 22 18 26
			//使用byte[]进行存储时需要 -128~+127
		    //对于年份使用两个byte存储
			for(int dataIndex=0;dataIndex<rawDataSize;dataIndex++)
			{
				int dateTemp=Integer.parseInt(currentDateTimeSplit[dataIndex]);
				if(dataIndex==0)
				{
					byte H8bits=(byte)((dateTemp)>>8);
					byte L8bits=(byte)((dateTemp)&0xff);
					dateTimeBytes[dataIndex]= H8bits;
					dateTimeBytes[dataIndex+1]= L8bits;
				}
				dateTimeBytes[dataIndex+1]=(byte)dateTemp;
			}
		}else
		{
			System.out.println("当前时间获取出现异常数据");
			System.exit(-1);
			dateTimeBytes=null;
		}
		return dateTimeBytes;
	}
	/*
	 * 对时间格式进行解析并还原原来的时间格式
	 * 对数据进行还原
	 * 仅限于debug使用
	 */
	public static String dateTimeBytesfromTostring(byte[] currentDateTime)
	{
		String string="";
		if(currentDateTime.length==7)
		{
		  string=((currentDateTime[0]<<8)+bytetoUnsigendInt(currentDateTime[1]))+" "+currentDateTime[2]+" "+
		  currentDateTime[3]+" "+currentDateTime[4]+" "+currentDateTime[5]+" "+
		  currentDateTime[6];
		}

		return string;
	}
	
	/*
	 * 将byte转化为字符串
	 * 将有符号byte转化为无符号数字
	 * debug使用
	 */
	public  static int bytetoUnsigendInt(byte aByte)
	{   
		
		String s=String.valueOf(aByte);
		System.out.println(s);
		//System.out.println(s);
		int bytetoUnsigendInt=0;
		for(int i=0;i<s.length();i++)
		{
			if(s.charAt(i)!='0')
			{
				bytetoUnsigendInt+=1<<(7-i);
			}
		}
		return bytetoUnsigendInt;
	}
	/*
	* 将数组封装成帧
	* 每一个数据帧由以下几个部分组成
	* 1)数据包头部 head 0X2F
	* 2)数据包命令 CMD  0X5A
	* 3)数据个数     length of data 7
	* 4)校验和         H8/L8 byte of  check sum(高字节在前 低字节在后)
	* 5)数据结尾标志 tail OX30
	* 6)可采用线程进行获取当前时间
	*/
	public static byte[] makeCurrentDateTimefromStringtoFramePackage(byte[] dateTimeBytes)
	{
		//在时间byte[]前后添加一些package校验信息
		int dataLength=13;
		byte[] terimalTimePackage=new byte[dataLength];
		//装填信息
		//时间数据包之前的信息
		terimalTimePackage[0]=0x2F;
		terimalTimePackage[1]=0X5A;
		terimalTimePackage[2]=7;
		//计算校验和
		//转化为无符号进行校验
		for(int dataIndex=0;dataIndex<dateTimeBytes.length;dataIndex++)
		{
			terimalTimePackage[dataIndex+3]=dateTimeBytes[dataIndex];
		}
		//将校验和分为高低字节
		byte sumH8bits=(byte)((timeCheckSum)>>8);
		byte sumL8bits=(byte)((timeCheckSum)&0xff);
		terimalTimePackage[10]=sumH8bits;//高字节在前
		terimalTimePackage[11]=sumL8bits;//低字节在后
		//数据包结尾
		terimalTimePackage[12]=0X30;
		return terimalTimePackage;
	}
}

