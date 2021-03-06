package org.turing.pangu.zzztest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.turing.pangu.bean.VpnConnectInfo;
/*
 * vpn 连接工具
 *
 * */
public class VpnUtil {
	 public static VpnUtil vpn = new VpnUtil();
	 public static VpnUtil getInstance()
	 {
		 if(null == vpn)
			 return new VpnUtil();
		 
		 return vpn;
	 }
	 
	/*
	 * rasdial kuangyufei gxfc51 6799 Add-VpnConnection kuangyufei yh.5jwl.net
	 * Add-VpnConnection -Name "kuangyufei" -ServerAddress "yh.5jwl.net"
	 */
	 
	public String createVpn(VpnConnectInfo info) {
		// Pptp L2tp
		InputStream in = null;
		BufferedReader reader = null;
		try {
			System.out.println("创建连接--" + info.getIp() + " -- " + info.getVpnName());
			StringBuffer buf = new StringBuffer();
			buf.append(" Add-VpnConnection ");
			buf.append(" -Name " + info.getVpnName());
			buf.append(" -ServerAddress " + info.getIp());
			buf.append(" -TunnelType " + info.getTunnelType());
			
			String tempCmd = executeCmd(buf.toString());
			System.out.println(tempCmd);
			return tempCmd;
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return "";
	}
	
	public String editVpn(VpnConnectInfo info) {
		// Pptp L2tp
		InputStream in = null;
		BufferedReader reader = null;
		try {
			System.out.println("编辑连接 --" + info.getIp()+ " -- " + info.getVpnName());
			StringBuffer buf = new StringBuffer();
			buf.append(" Set-VpnConnection ");
			buf.append(" -Name " + info.getVpnName());
			buf.append(" -ServerAddress " + info.getIp());
			buf.append(" -TunnelType " + info.getTunnelType());
			buf.append(" -EncryptionLevel " + info.getEncryptionLevel());
			buf.append(" -AuthenticationMethod " + info.getAuthenticationMethod());
			String tempCmd = executeCmd(buf.toString());
			System.out.println(tempCmd);
			return tempCmd;
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return "";
	}
	public String getAllUserConnection() {
		InputStream in = null;
		BufferedReader reader = null;
		try {
			String cmd = " Get-VpnConnection -AllUserConnection ";
			String tempCmd = executeCmd(cmd);
			return tempCmd;
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return "";
	}
	public String getVpn(VpnConnectInfo info) {
		InputStream in = null;
		BufferedReader reader = null;
		try {
			System.out.println("获取 --" + info.getVpnName());
			String cmd = " Get-VpnConnection " + info.getVpnName();
			String tempCmd = executeCmd(cmd);
			System.out.println(tempCmd);
			return tempCmd;
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return "";
	}
	
	public String removeVpn(VpnConnectInfo info) {
		InputStream in = null;
		BufferedReader reader = null;
		try {
			System.out.println("删除连接 -- " + info.getVpnName());
			String cmd = " Remove-VpnConnection " + info.getVpnName();
			String tempCmd = executeCmd(cmd);
			System.out.println(tempCmd);
			return tempCmd;
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return "";
	}

	/**
	 * 连接ADSL 语法： rasdial 连接名称 username password 实例： rasdial 我的宽带 hzhz1234567890
	 * dfdfdfdfdf rasdial turing gxfc51 6799
	 */
	public String connectVpn(VpnConnectInfo info) throws Exception {
		System.out.println("正在建立连接." + info.getVpnName() + " " + info.getUserName() + " " + info.getPassword());
		String adslCmd = "rasdial " + info.getVpnName() + " " + info.getUserName() + " " + info.getPassword();
		String result = executeCmd(adslCmd);
		System.out.println(result);
		return result;
		/*
		// 判断是否连接成功
		if (tempCmd.indexOf("已连接") > 0) {
			System.out.println("已成功建立连接.");
			return true;
		} else {
			System.err.println(tempCmd);
			System.err.println("建立连接失败");
			return false;
		}*/
	}

	/**
	 * 断开ADSL rasdial turing /disconnect
	 */
	public String cutVpn(VpnConnectInfo info) throws Exception {
		String cutAdsl = "rasdial " + info.getVpnName() + " /disconnect";
		String result = executeCmd(cutAdsl);
		return result;
	}
	
	/**
	 * 执行CMD命令,并返回String字符串
	 */
	private static String executeCmd(String strCmd) throws Exception {
		Process p = Runtime.getRuntime().exec("cmd /c powershell " + strCmd);
		StringBuilder sbCmd = new StringBuilder();
		// 这里很重要，设置GB2312解决乱码！！！
		// 如果程序默认编码就是GB2312，可以不写
		// 我NetBeans默认用UTF8
		BufferedReader br = new BufferedReader(new InputStreamReader(
				p.getInputStream(), "GB2312"));
		String line;
		while ((line = br.readLine()) != null) {
			sbCmd.append(line + "\n");
		}
		return sbCmd.toString();
	}

}