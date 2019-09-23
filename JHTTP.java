
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
public class JHTTP {
	private static final Logger logger = Logger.getLogger(JHTTP.class.getCanonicalName());
	private static final int NUM_THREADS = 50;
	private static final String INDEX_FILE = "index.html";
	private final File rootDirectory;
	private final int port;
	public JHTTP(File rootDirectory, int port) throws IOException{
		if(!rootDirectory.isDirectory()) {
			throw new IOException(rootDirectory + "does not exist as a directory");
		}
		this.rootDirectory = rootDirectory;
		this.port = port;
	}
	public void start() throws IOException{
		ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
		try(ServerSocket server = new ServerSocket(port)){
			logger.info("Accepting connections on port" + server.getLocalPort());
			logger.info("Document Root:" + rootDirectory);
			while(true) {
				try {
					Socket request = server.accept();
					Runnable r = new RequestProcessor(rootDirectory,INDEX_FILE,request);
					pool.submit(r);
				}catch(IOException ex) {
					logger.log(Level.WARNING,"Error accepting connection",ex);
				}
			}
		}
	}
    public static void main(String[] args) {
    	File docroot = new File("E:\\HTTPServer");
    	int port = 80;
    	try {
    		JHTTP webserver = new JHTTP(docroot,port);
    		webserver.start();
    	}catch(IOException ex) {
    		logger.log(Level.SEVERE, "Server could not start", ex);
    	}
    }
}
class RequestProcessor implements Runnable{
	private final static Logger logger = Logger.getLogger(RequestProcessor.class.getCanonicalName());
	private File rootDirectory;
	private String indexFileName = "index.html";
	private Socket connection;
	public RequestProcessor(File rootDirectory,String indexFileName,Socket connection) {
		if(rootDirectory.isFile()) {
			throw new IllegalArgumentException("rootDirectory must be a directory, not a file");
		}
		try {
			rootDirectory = rootDirectory.getCanonicalFile();
		}catch(IOException ex) {
		}
		this.rootDirectory = rootDirectory;
		if(indexFileName!=null) {
			this.indexFileName = indexFileName;
		}
		this.connection = connection;
	}
	@Override
	public void run() {
		String root = rootDirectory.getPath();
		try {
			OutputStream raw = new BufferedOutputStream(connection.getOutputStream());
			Writer out = new OutputStreamWriter(raw);
			InputStream is = connection.getInputStream();
			DataInputStream dis = new DataInputStream(is);
			StringBuilder requestLine = new StringBuilder();
			while(true) {
				int c = is.read();
				if(c=='\r'||c=='\n') {
					break;
				}
				requestLine.append((char)c);
			}
			String get = requestLine.toString();
			logger.info(connection.getRemoteSocketAddress() + " " + get);
			String[] tokens = get.split("\\s+");
			String method = tokens[0];
			String version = "";
			if(method.equals("GET")) {
				dis.readLine();
				String login;
				while(!(login = dis.readLine()).equals("")) {
					System.out.println(login);
				}
				String fileName = tokens[1];
				if(fileName.endsWith("/")) {
					fileName += indexFileName;
				}
				String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
				if(tokens.length>2) {
					version = tokens[2];
				}
				File theFile = new File(rootDirectory,fileName.substring(1,fileName.length()));
				if(theFile.canRead()&&theFile.getCanonicalPath().startsWith(root)) {
					byte[] theData = Files.readAllBytes(theFile.toPath());
					if(version.startsWith("HTTP/")) {
						sendHeader(out,"HTTP/1.1 200 OK",contentType,theData.length);
					}
					raw.write(theData);
					raw.flush();
				}else {
					String body = new StringBuilder("<HTML>\r\n<HEAD><TITLE>File Not Found</TITLE>\r\n</HEAD>\r\n<BODY><H1>HTTP Error 404: File Not Found</H1>\r\n</BODY></HTML>\r\n").toString();
					if(version.startsWith("HTTP/")) {
						sendHeader(out,"HTTP/1.1 404 File Not Found","text/html; charset=utf-8",body.length());
					}
					out.write(body);
					out.flush();
				}
			}else if(method.equals("HEAD")){
				String fileName = tokens[1];
				if(fileName.endsWith("/")) {
					fileName += indexFileName;
				}
				String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
				if(tokens.length>2) {
					version = tokens[2];
				}
				File theFile = new File(rootDirectory,fileName.substring(1,fileName.length()));
				if(theFile.canRead()&&theFile.getCanonicalPath().startsWith(root)) {
					byte[] theData = Files.readAllBytes(theFile.toPath());
					if(version.startsWith("HTTP/")) {
						sendHeader(out,"HTTP/1.1 200 OK",contentType,theData.length);
					}
					raw.flush();
				}else {
					String body = new StringBuilder("<HTML>\r\n<HEAD><TITLE>File Not Found</TITLE>\r\n</HEAD>\r\n<BODY><H1>HTTP Error 404: File Not Found</H1>\r\n</BODY></HTML>\r\n").toString();
					if(version.startsWith("HTTP/")) {
						sendHeader(out,"HTTP/1.1 404 File Not Found","text/html; charset=utf-8",body.length());
					}
					out.flush();
				}
			}else if(method.equals("POST")){
				int contentLength = 0;
				dis.readLine();
				String login;
				while(!(login = dis.readLine()).equals("")) {
					if (login.startsWith("Content-Length")) {  
		                contentLength = Integer.parseInt(login.split(":")[1].trim());  
		            }
				}
				StringBuilder content = new StringBuilder();
				while(contentLength-->0) {
					int c = is.read();
					content.append((char)c);
				}
				if(content.toString().contains("name=dddder4")&&content.toString().contains("password=123456")) {
					String fileName = tokens[1];
					if(fileName.endsWith("/")) {
						fileName += indexFileName;
					}
					String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
					if(tokens.length>2) {
						version = tokens[2];
					}
					File theFile = new File(rootDirectory,fileName.substring(1,fileName.length()));
					if(theFile.canRead()&&theFile.getCanonicalPath().startsWith(root)) {
						byte[] theData = Files.readAllBytes(theFile.toPath());
						if(version.startsWith("HTTP/")) {
							sendHeader(out,"HTTP/1.1 200 OK",contentType,theData.length);
						}
						raw.write(theData);
						raw.flush();
					}else {
						String body = new StringBuilder("<HTML>\r\n<HEAD><TITLE>File Not Found</TITLE>\r\n</HEAD>\r\n<BODY><H1>HTTP Error 404: File Not Found</H1>\r\n</BODY></HTML>\r\n").toString();
						if(version.startsWith("HTTP/")) {
							sendHeader(out,"HTTP/1.1 404 File Not Found","text/html; charset=utf-8",body.length());
						}
						out.write(body);
						out.flush();
					}
				}else {
					String body = new StringBuilder("<HTML>\r\n<HEAD><TITLE>Unauthorized</TITLE>\r\n</HEAD>\r\n<BODY><H1>HTTP Error 401: Unauthorized</H1>\r\n</BODY></HTML>\r\n").toString();
					if(version.startsWith("HTTP/")) {
						sendHeader(out,"HTTP/1.1 401 Unauthorized","text/html; charset=utf-8",body.length());
					}
					out.write(body);
					out.flush();
				}
			}else if(method.equals("PUT")){
				int contentLength = 0;
				dis.readLine();
				String login;
				while(!(login = dis.readLine()).equals("")) {
					if (login.startsWith("Content-Length")) {
		                contentLength = Integer.parseInt(login.split(":")[1].trim());  
		            }
				}
				String fileName = tokens[1];
				String[] path = fileName.substring(1,fileName.length()).split("/");
				File theFile = rootDirectory;
				if(path.length>1) {
					for(int i=0;i<path.length-1;i++) {
						theFile = new File(theFile,path[i]);
						theFile.mkdir();
					}
				}
				theFile = new File(theFile,path[path.length-1]);
				FileOutputStream fos = new FileOutputStream(theFile);
				while(contentLength-->0) {
					fos.write(is.read());
				}
				String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
				if(tokens.length>2) {
					version = tokens[2];
				}
				byte[] theData = Files.readAllBytes(theFile.toPath());
				if(version.startsWith("HTTP/")) {
					sendHeader(out,"HTTP/1.1 200 OK",contentType,theData.length);
				}
				raw.flush();
			}else {
				String body = new StringBuilder("<HTML>\r\n<HEAD><TITLE>Not Implemented</TITLE>\r\n</HEAD>\r\n<BODY><H1>HTTP Error 501: Not Implemented</H1>\r\n</BODY></HTML>\r\n").toString();
				if(version.startsWith("HTTP/")) {
					sendHeader(out,"HTTP/1.1 501 Not Implemented","text/html; charset=utf-8",body.length());
				}
				out.write(body);
				out.flush();
			}
		}catch(IOException ex) {
				logger.log(Level.WARNING,"Error talking to "+connection.getRemoteSocketAddress(),ex);
		}finally {
			try {
				connection.close();
			}catch(IOException ex) {
			}
		}
	}
	private void sendHeader(Writer out,String responseCode,String contentType,int length)throws IOException{
		out.write(responseCode + "\r\n");
		Date now = new Date();
		out.write("Date: " + now + "\r\n");
		out.write("Server: JHTTP 2.0\r\n");
		out.write("Content-length: " + length + "\r\n");
		out.write("Content-type: " + contentType + "\r\n\r\n");
		out.flush();
	}
}