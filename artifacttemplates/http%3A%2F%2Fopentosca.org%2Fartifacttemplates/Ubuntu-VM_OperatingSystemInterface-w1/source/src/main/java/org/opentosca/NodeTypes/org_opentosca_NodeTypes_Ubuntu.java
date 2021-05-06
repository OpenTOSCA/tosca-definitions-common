package org.opentosca.NodeTypes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.apache.commons.io.FileUtils;
import org.vngx.jsch.Session;
import org.vngx.jsch.exception.JSchException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;

import ssh.SSHRemoteCommandExec;
import ssh.SSHRemoteFileTransfer;
import ssh.SSHSessionFactory;

@WebService(targetNamespace = "http://implementationartifacts.opentosca.org/")
public class org_opentosca_NodeTypes_Ubuntu extends AbstractIAService {
	
	
	public static final String MSG_FAILED = "FAILED";
	public static final String TESTMODE = "TESTMODE";
	
	
	@WebMethod
	@SOAPBinding
	@Oneway
	public void installPackage(@WebParam(name = "VMIP", targetNamespace = "http://implementationartifacts.opentosca.org/") String IP, @WebParam(name = "VMUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String User, @WebParam(name = "VMPrivateKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String PrivateKey, @WebParam(name = "PackageNames", targetNamespace = "http://implementationartifacts.opentosca.org/") String PackageNames) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();
		
		Session session = SSHSessionFactory.createSSHSession(IP.trim(), User.trim(), PrivateKey.trim());
		
		// just to be sure the packages will be installed either with apt-get or
		// yum
		String installPackageScript = "(sudo apt-get update && sudo apt-get -y install " + PackageNames + ") || (sudo yum update && sudo yum -y install " + PackageNames + ")";
		String res = this.runScript(session, installPackageScript).trim();
		// Output Parameter 'success' (optional)
		// Do NOT delete the next line of code. Set "" as value if you want to
		// return nothing or an empty result!
		if (res.endsWith("Complete!") || res.endsWith("Nothing to do")) {
			returnParameters.put("InstallResult", "1");
			
		} else {
			returnParameters.put("InstallResult", "0");
		}
		
		sendResponse(returnParameters);
	}
	
	@WebMethod
	@SOAPBinding
	@Oneway
	public void transferFile(@WebParam(name = "VMIP", targetNamespace = "http://implementationartifacts.opentosca.org/") String IP, @WebParam(name = "VMUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String User, @WebParam(name = "VMPrivateKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String PrivateKey, @WebParam(name = "TargetAbsolutePath", targetNamespace = "http://implementationartifacts.opentosca.org/") String TargetAbsolutePath, @WebParam(name = "SourceURLorLocalPath", targetNamespace = "http://implementationartifacts.opentosca.org/") String SourceURLorLocalPath) {
		// This HashMap holds the return parameters of this operation.
		HashMap<String, String> returnParameters = new HashMap<String, String>();
		
		// old way
		try {
			// Transform sourceURLorLocalAbsolutePath to URL
			URL url = null;
			try {
				// Check if the string is a URL right away?
				url = new URL(SourceURLorLocalPath);
			} catch (Exception e) {
				// It's not a URL
				// Check if string is a local path
				File file = new File(SourceURLorLocalPath);
				if (file.exists()) {
					try {
						url = file.toURI().toURL();
						
					} catch (Exception e2) {
						// FAILED: Return async message
						// Also problem processing as file, giving up
						throw new Exception("TRANSFER_FAILED with the old way: File " + SourceURLorLocalPath + " is no valid URL and does not exist on the local file system. (Exception: " + e + ")");
					}
				} else {
					// FAILED: Return async message
					throw new Exception("TRANSFER_FAILED with the old way: File " + SourceURLorLocalPath + " is no valid URL and does not exist on the local file system.");
				}
			}
			
			// Resolve to user home if remote path starts with ~
			if (TargetAbsolutePath.startsWith("~")) {
				Session session = SSHSessionFactory.createSSHSession(IP, User, PrivateKey);
				String pwd = this.runScript(session, "pwd").trim();
				TargetAbsolutePath = TargetAbsolutePath.replaceFirst("~", pwd);
				System.out.println("Replaced ~ with user home ('" + pwd + "'): '" + TargetAbsolutePath + "'");
				session.disconnect();
			}
			
			// Opens stream and uploads file
			try {
				// If there is no output stream a HTTP GET is done by default
				HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
				httpConnection.setRequestProperty("Accept", "application/octet-stream");
				InputStream inputStream = httpConnection.getInputStream();
				
				Session session = SSHSessionFactory.createSSHSession(IP, User, PrivateKey);
				SSHRemoteFileTransfer transfer = new SSHRemoteFileTransfer(session);
				transfer.putFile(inputStream, TargetAbsolutePath);
				transfer.close();
				
				inputStream.close();
				httpConnection.disconnect();
				session.disconnect();
				
				returnParameters.put("TransferResult", "sucessfull");
				
			} catch (Exception e) {
				throw new Exception("TRANSFER_FAILED with the old way: " + e);
			}
		} 
		
		// new Jsch
		catch (Exception eOld) {
			
			System.out.println("Failed the old way with error:\n   " + eOld.getLocalizedMessage());
			
			returnParameters = new HashMap<String, String>();
			
			// Transform sourceURLorLocalAbsolutePath to URL
			URL url = null;
			try {
				// Check if the string is a URL right away?
				url = new URL(SourceURLorLocalPath);
			} catch (Exception e) {
				// It's not a URL
				// Check if string is a local path
				File file = new File(SourceURLorLocalPath);
				if (file.exists()) {
					try {
						url = file.toURI().toURL();
						
					} catch (Exception e2) {
						// FAILED: Return async message
						// Also problem processing as file, giving up
						returnParameters.put("TransferResult", "TRANSFER_FAILED: File " + SourceURLorLocalPath + " is no valid URL and does not exist on the local file system. (Exception: " + e + ")");
						sendResponse(returnParameters);
						return;
					}
				} else {
					// FAILED: Return async message
					returnParameters.put("TransferResult", "TRANSFER_FAILED: File " + SourceURLorLocalPath + " is no valid URL and does not exist on the local file system.");
					sendResponse(returnParameters);
					return;
				}
			}
			
			try {
				
				System.out.println("initialize Jsch session");
				com.jcraft.jsch.Session session = jschConnect(IP, User, PrivateKey);
				
				// Resolve to user home if remote path starts with ~
				if (TargetAbsolutePath.startsWith("~")) {
					String pwd = jschRunScript("pwd", session).trim();
					TargetAbsolutePath = TargetAbsolutePath.replaceFirst("~", pwd);
					System.out.println("Replaced ~ with user home ('" + pwd + "'): '" + TargetAbsolutePath + "'");
					// session.disconnect();
				} else {
					System.out.println("target path does not start with a ~");
				}
				
				// Opens stream and uploads file
				// If there is no output stream a HTTP GET is done by default
				HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
				httpConnection.setRequestProperty("Accept", "application/octet-stream");
				InputStream inputStream = httpConnection.getInputStream();
				
				ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
				sftp.connect();
				
				sftp.put(inputStream, TargetAbsolutePath);
				
				inputStream.close();
				sftp.disconnect();
				httpConnection.disconnect();
				session.disconnect();
				
				returnParameters.put("TransferResult", "sucessfull");
				
			} catch (Exception e) {
				e.printStackTrace();
				returnParameters.put("TransferResult", "TRANSFER_FAILED: " + e);
			}
			
		}
		
		// Returning a parameter is required so that we can wait asynchronously
		// in the process.
		
		sendResponse(returnParameters);
	}
	
	@WebMethod
	@SOAPBinding
	@Oneway
	public void runScript(@WebParam(name = "VMIP", targetNamespace = "http://implementationartifacts.opentosca.org/") String IP, @WebParam(name = "VMUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String User, @WebParam(name = "VMPrivateKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String PivateKey, @WebParam(name = "Script", targetNamespace = "http://implementationartifacts.opentosca.org/") String Script) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();
		String res;
		
		try {
			Session session = SSHSessionFactory.createSSHSession(IP, User, PivateKey);
			res = this.runScript(session, Script);
			session.disconnect();
		}catch(Exception e){
			System.out.println("Failed with old method, thus try new Jsch.");
			res = runScriptNew(IP, User, PivateKey, Script);
		}
		returnParameters.put("ScriptResult", res);
		
		sendResponse(returnParameters);
	}
	
	@WebMethod
	@SOAPBinding
	@Oneway
	public void waitForAvailability(@WebParam(name = "VMIP", targetNamespace = "http://implementationartifacts.opentosca.org/") String IP, @WebParam(name = "VMUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String User, @WebParam(name = "VMPrivateKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String PrivateKey) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();
		
		// Testmode
		if (IP.equals(TESTMODE)) {
			System.out.println("##### " + TESTMODE + " ##### ");
			try {
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Output Parameter 'pwd' (optional)
			// Do NOT delete the next line of code. Set "" as value if you want
			// to return nothing or an empty result!
			returnParameters.put("WaitResult", TESTMODE);
			sendResponse(returnParameters);
			return;
		}
		
		// Try to connect to SSH port (for approx. 16 min)
		int maxTriesSocket = 100;
		for (int i = 1; i <= maxTriesSocket; i++) {
			if (isSSHServiceUp(IP)) {
				break;
			}
			
			System.out.println("Waiting for SSH service to come up... (" + i + " of max. " + maxTriesSocket + ")");
			
			// Wait
			try {
				Thread.sleep(5 * 1000); // wait for 5 sec
			} catch (InterruptedException e) {
				// we just go on in this case.
			}
		}
		System.out.println("SSH service is up, try to login.");
		
		// Try to establish SSH connection
		String pwd = "";
		int maxTriesSSHLogin = 25;
		boolean switcher = true;
		for (int i = 1; i <= maxTriesSSHLogin; i++) {
			String pwdRespone = null;
			
			if (switcher) {
				pwdRespone = isSSHLoginPossibleNewJsch(IP, User, PrivateKey);
			} else {
				pwdRespone = isSSHLoginPossible(IP, User, PrivateKey);
			}
			
			switcher = !switcher;
			
			System.out.println("Return of ssh login check: " + pwdRespone);
			
			if (!pwdRespone.equals(MSG_FAILED)) {
				System.out.println(pwdRespone);
				pwd = pwdRespone;
				break;
			}
			
			System.out.println("Waiting for successfull SSH login... (" + i + " of max. " + maxTriesSSHLogin + ")");
			
			// Wait
			try {
				Thread.sleep(5 * 1000); // wait for 5 sec
			} catch (InterruptedException e) {
				// we just go on in this case.
			}
		}
		System.out.println("Ubuntu VM started and SSH is ready.");
		
		// Returning a parameter is required so that we can wait asynchronously
		// in the process.
		
		// Output Parameter 'pwd' (optional)
		// Do NOT delete the next line of code. Set "" as value if you want to
		// return nothing or an empty result!
		
		returnParameters.put("WaitResult", pwd);
		
		sendResponse(returnParameters);
	}
	
	/**
	 * Checks if the login via SSH is possible.
	 *
	 * @param hostname
	 * @param sshUser
	 * @param sshKey
	 * @return
	 */
	private String isSSHLoginPossible(String hostname, String sshUser, String sshKey) {
		
		System.out.println("try with old way");
		
		// Try to establish SSH connection
		try {
			Session session = SSHSessionFactory.createSSHSession(hostname, sshUser, sshKey);
			SSHRemoteCommandExec cmd = new SSHRemoteCommandExec(session);
			/**
			 * Because of this problem we have to wait some time after we can
			 * declare the VM to be started up, because there might be running
			 * some background scripts, e.g. at Amazon to configure the APT get
			 * repositories
			 * http://serverfault.com/questions/440569/apt-get-update
			 * -directly-after-boot-results-in-many-ign-and-hit-resulting-in-no
			 */
			String res = cmd.execute("pwd; sleep 1;");
			cmd.close();
			return res;
		} catch (Exception e) {
			return MSG_FAILED;
		}
	}
	
	/**
	 * Checks if the SSH port allows a socket connection which indicated that
	 * the SSH service has been started.
	 *
	 * @return true if a socket connection could be created, false otherwise.
	 */
	private boolean isSSHServiceUp(String hostname) {
		// Try to open socket
		try {
			Socket s = new Socket(hostname, 22);
			s.close();
			return true;
			
		} catch (UnknownHostException e1) {
			// this exception is expected
			return false;
			
		} catch (IOException e1) {
			// this exception is expected
			return false;
		}
	}
	
	/**
	 * Run Script wrapper which returns the console output produced by the
	 * script.
	 *
	 * @param session
	 * @param script
	 * @return
	 */
	private String runScript(Session session, String script) {
		try {
			SSHRemoteCommandExec cmd = new SSHRemoteCommandExec(session);
			String res = cmd.execute(script);
			cmd.close();
			return res;
		} catch (JSchException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String runScriptNew(String hostname, String sshUser, String sshKey, String script) {
		
		System.out.println("try with Jsch");
		
		try {
			
			com.jcraft.jsch.Session session = jschConnect(hostname, sshUser, sshKey);
			String ret = jschRunScript(script, session);
			session.disconnect();
			return ret;
			
		} catch (com.jcraft.jsch.JSchException e) {
			System.out.println("Jsch did not accept the ssh key: " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not create tmp file for ssh key: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		return MSG_FAILED;
	}
	
	/**
	 * Found at
	 * http://stackoverflow.com/questions/4932005/can-we-use-jsch-for-ssh-key-
	 * based-communication
	 * 
	 * @param hostname
	 * @param sshUser
	 * @param sshKey
	 * @return
	 */
	private String isSSHLoginPossibleNewJsch(String hostname, String sshUser, String sshKey) {
		
		System.out.println("try with Jsch");
		
		try {
			com.jcraft.jsch.Session session = jschConnect(hostname, sshUser, sshKey);
			if (session.isConnected()) {
				session.disconnect();
				return "connected with Jsch";
			}
			session.disconnect();
			
		} catch (com.jcraft.jsch.JSchException e) {
			System.out.println("Jsch did not accept the ssh key: " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not create tmp file for ssh key: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		return MSG_FAILED;
	}
	
	private com.jcraft.jsch.Session jschConnect(String hostname, String sshUser, String sshKey) throws IOException, com.jcraft.jsch.JSchException {
		JSch jsch = new JSch();
		File key = File.createTempFile("key", "tmp", FileUtils.getTempDirectory());
		FileUtils.write(key, sshKey, "UTF-8");
		System.out.println("tmp key file created: " + key.exists());
		
		jsch.addIdentity(key.getAbsolutePath());
		com.jcraft.jsch.Session session = jsch.getSession(sshUser, hostname);
		
		FileUtils.forceDelete(key);
		System.out.println("tmp key file deleted: " + !key.exists());
		
		// disabling StrictHostKeyChecking may help to make connection but
		// makes it insecure
		// see
		// http://stackoverflow.com/questions/30178936/jsch-sftp-security-with-session-setconfigstricthostkeychecking-no
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect();
		return session;
	}
	
	private String jschRunScript(String script, com.jcraft.jsch.Session session) throws com.jcraft.jsch.JSchException, IOException {
		if (session.isConnected()) {
			
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(script);
			
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			
			InputStream is = channel.getInputStream();
			channel.connect();
			byte[] tmp=new byte[1024];
			
			//			StringWriter writer = new StringWriter();
			String res = null;
			while (true) {
				
				while(is.available()>0){
					int i=is.read(tmp, 0, 1024);
					if(i<0)break;
					res = new String(tmp, 0, i);
				}
				
				if (channel.isClosed()) {
					System.out.println("exit-status: " + channel.getExitStatus());
					
					//					System.out.println("transfer script invocation response to return");
					//					
					//					IOUtils.copy(is, writer, "utf8");
					//					
					//					System.out.println("Done.");
					
					break;
				}
				try {
					System.out.println("sleep for waiting for result of script execution.");
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			channel.disconnect();
			
			//			String res = writer.toString().replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "");
			System.out.println(res);
			return res;
		}
		return MSG_FAILED;
	}
	
}
