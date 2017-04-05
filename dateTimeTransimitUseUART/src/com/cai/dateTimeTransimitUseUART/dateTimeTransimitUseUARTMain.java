package com.cai.dateTimeTransimitUseUART;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import javax.sql.rowset.serial.SerialException;
/*
 * 主要是实现对系统时间的数据包封装通过windows UART传送给单板机进行时间校准
 * 本程序主要是实现上位机程序，实现对时间数据包的数据的格式的定义与解释
 * 根据定义的时间packageg格式在单板机上进行解析
 * Version 1.0
 * Author：caizq
 * date time:2016/12/23
 * location：tju lab349
 * 逐步实现以下
 * step 1:加载UART 通信数据包
 * step 2:单列模式获取系统时间(如何获取？方案)
 * step 3:开启线程，该线程主要负责取时间后将时间数据包进行发送至串口
 * step 4:简单的测试代码(可以使用线程监控串口buffer数据并显示,尝试解析并打印日志)
 * step 5:实现界面的封装
 * step 6:添加UART接收程序
 * step 7:对一些波形进行简单显示以及对数据进行存储
 * step 8:mysql数据进行存储 JDBC
 * step 9:进一步实现界面的封装
 * step 10:对重要信息的可视化
 */
public class dateTimeTransimitUseUARTMain
{
	public static void main(String[] args) 
	{
		//主mian类
		byte[] dataFrame={0x00,0x5A,0x64,0x56,0x43,0x6F,0x78};
		ArrayList<String> arraylist=UARTParameterSetup.uartPortUseAblefind();
		int useAbleLen=arraylist.size();
		if(useAbleLen==0)
		{
			System.out.println("没有找到可用的串口端口，请check设备！");
		}
		else
		{   
			System.out.println("已查询到该计算机上有以下端口可以使用：");
			for(int index=0;index<arraylist.size();index++)
			{
				System.out.println("该COM端口名称:"+arraylist.get(index));
				//测试串口配置的相关方法
			} 
			//取出第一个COM端口进行测试
			SerialPort serialPort=UARTParameterSetup.portParameterOpen(arraylist.get(0), 57600);
		    //退出程序 后续不需要监测 因为transimit一直需要保证连接状态
			//System.exit(0);
			DataTransimit.uartSendDatatoSerialPort(serialPort, dataFrame);
			String currentDateTime=SystemDateTimeGet.getCurrentDateTime();
			System.out.println(currentDateTime);
			byte[] bytes=SystemDateTimeGet.dateTimeBytesGet(currentDateTime);
			//System.out.println(Arrays.toString(bytes));
			String str=SystemDateTimeGet.dateTimeBytesfromTostring(bytes);
			System.out.println(str);
			//System.out.println(SystemDateTimeGet.bytetoUnsigendInt((byte) -32));
			byte[] terimalTimeByte=SystemDateTimeGet.makeCurrentDateTimefromStringtoFramePackage(bytes);
			System.out.println(Arrays.toString(terimalTimeByte));
			DataTransimit.uartSendDatatoSerialPort(serialPort, terimalTimeByte);
			//关闭串口
			UARTParameterSetup.closePort(serialPort);
		}		
	}
}
/*定义一个串口数据包的类并做一些简单的操作
 * 主要是串口通信的一些异常检测以及提示
 * 对通信串口的一些基本参数的设置
 * 通常将外部引用的jar包存在工程下lib文件下
 * 该类的几个方法都是对串口的检测与设置，不需要经常修改 属于一个类的操作因此使用static类方法
 * 设置继承权限，不希望被扩展类继承，因此类的修饰符为：final
 */
class UARTParameterSetup
{
	/*类方法 不可改变 不接受继承
	 * 扫描获取可用的串口
	 * 将可用串口添加至list并保存至list
	 */
	public static final ArrayList<String> uartPortUseAblefind()
	{
		//获取当前所有可用串口 
		//由CommPortIdentifier类提供方法
		Enumeration<CommPortIdentifier> portList=CommPortIdentifier.getPortIdentifiers();
		ArrayList<String> portNameList=new ArrayList();
		//添加并返回ArrayList
		while(portList.hasMoreElements())
		{
			String portName=portList.nextElement().getName();
			portNameList.add(portName);	
		}
		return portNameList;
	}
	/*
	 * 串口常见设置
	 * 1)打开串口
	 * 2)设置波特率 根据单板机的需求可以设置为57600 ...
	 * 3)判断端口设备是否为串口设备
	 * 4)端口是否占用
	 * 5)对以上条件进行check以后返回一个串口设置对象new UARTParameterSetup()
	 * 6)return:返回一个SerialPort一个实例对象，若判定该com口是串口则进行参数配置
	 *   若不是则返回SerialPort对象为null
	 */
	public static final SerialPort portParameterOpen(String portName,int baudrate)
	{
		SerialPort serialPort=null;
		try 
		{  //通过端口名识别串口
		   CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		   //打开端口并设置端口名字 serialPort和超时时间 2000ms
		   CommPort commPort=portIdentifier.open(portName,1000);
		   //进一步判断comm端口是否是串口 instanceof
		   if(commPort instanceof SerialPort)
		   {
			   System.out.println("该COM端口是串口！");
			   //进一步强制类型转换
			   serialPort=(SerialPort)commPort;
			   //设置baudrate 此处需要注意:波特率只能允许是int型 对于57600足够
			   //8位数据位
			   //1位停止位
			   //无奇偶校验
			   serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8,SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			   //串口配制完成 log
			   System.out.println("串口参数设置已完成，波特率为"+baudrate+",数据位8bits,停止位1位,无奇偶校验");
		   }
		   //不是串口
		   else
		   {
			   System.out.println("该com端口不是串口,请检查设备!");
			   //将com端口设置为null 默认是null不需要操作
		   }
			  
		} 
		catch (NoSuchPortException e) 
		{
			e.printStackTrace();
		} 
		catch (PortInUseException e) 
		{
			e.printStackTrace();
		} 
		catch (UnsupportedCommOperationException e)
        {
			e.printStackTrace();
		}
		
		return serialPort;		
	}
    /*
     * 关闭串口
     * 串口关闭以及检测的COM端口非串口的标志是返回一个SerialPort是null
     * 关闭串口后使用null进行重置
     */
    public static void closePort(SerialPort serialPort)
    {
    	if(serialPort!=null)
    	{
    		serialPort.close();
    		serialPort=null;
    		System.out.println("串口已关闭！");
    	}
    }
     
}

/*
 * 串口数据发送以及数据传输作为一个类
 * 该类做主要实现对数据包的传输至下单板机
 */
class DataTransimit
{
	
	/*
	 * 上位机往单板机通过串口发送数据
	 * 串口对象 seriesPort 
	 * 数据帧:dataPackage
	 * 发送的标志:数据未发送成功抛出一个异常
	 */
	public static void uartSendDatatoSerialPort(SerialPort serialPort,byte[] dataPackage)
	{
		OutputStream out=null;
		try
		{
			out=serialPort.getOutputStream();
			out.write(dataPackage);
			out.flush();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}finally
		{
			//关闭输出流
			if(out!=null)
			{
				try 
				{
					out.close();
					out=null;
					System.out.println("数据已发送完毕!");
				} catch (IOException e) 
				{
					e.printStackTrace();
				}	
			}
		}			
     }
	/*
	 * 上位机接收数据
	 * 串口对象seriesPort
	 * 接收数据buffer
	 * 返回一个byte数组
	 */
	public  static  byte[] uartReceiveDatafromSingleChipMachine(SerialPort serialPort)
	{
		byte[] receiveDataPackage=null;
		InputStream in=null;
		try 
		{
			in=serialPort.getInputStream();
			//获取data buffer数据长度
			int bufferLength=in.available();
			while(bufferLength!=0)
			{
				receiveDataPackage=new byte[bufferLength];
				in.read(receiveDataPackage);
				bufferLength=in.available();
				
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return receiveDataPackage;
	}		
    /*
     * 监听器
     * 数据接收与通信时唤醒线程相关
     * 需要根据线程进行补充
     */
    public static void listener(SerialPort port,SerialPortEventListener listener)
    {
    	//串口添加监听器
    	try 
    	{
			port.addEventListener(listener);
		} catch (TooManyListenersException e)
		{
			e.printStackTrace();
		}
    	//设置当前有效
    }
}