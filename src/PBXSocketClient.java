import netscape.javascript.JSObject;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PBXSocketClient {
    String pbxIp = "PbxIP"; // It's not real IP
    String cmsPostAddress = "API Address"; // It's not real address
    static final int portNumber = 5050;
    Socket client = null;
    InputStream in;
    OutputStream out;
    PbxAccount pbxAccount;
    ReceiveDataThread receiveDataThread;


    public String getCurrentTime(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = new Date();
        return format.format(d);
    }

    public PBXSocketClient(UUID uuid, String id, String password, String extenNumber){
        this.pbxAccount = new PbxAccount(uuid, id,password,extenNumber);
    }

    public PBXSocketClient(PbxAccount pbxAccount){
        this.pbxAccount = pbxAccount;
    }

    public void run(){
        try {
            // socket client connection
            client = new Socket(pbxIp, portNumber);

            // make login packet
            String loginPacket = makeLoginPacket();

            in = client.getInputStream();
            out = client.getOutputStream();

            // send login request
            out.write(loginPacket.getBytes());
            out.flush();


            // ---------------- login success ---------------------

            byte[] recv = new byte[1000];

            while(true) {
                int k = in.read(recv);
                String recvString = new String(recv, 0, k);


                // if PACKET type is not C, throw it away
                if(!recvString.substring(0,2).equals("SC")) continue;
                int dataLen = Integer.parseInt(recvString.substring(2,5));
                if(dataLen == 0) continue;

                // get Parameters from Response Packet
                String data = recvString.substring(5,5+dataLen);
                String[] params = data.split("&");
                if(data.substring(0,6).equals("CSRALL")) continue;
                //System.out.println(data);

                if(params.length == 0) continue;

                // Log in Response
                if(params[0].equals("LOGIN_RES")){
                    if(params[1].equals("LOGIN_OK")){
                        System.out.println("[Login Status] " + pbxAccount.getAccount() + " : Login Success.");
                    }
                    else {
                        System.out.println("[Login Status] " + pbxAccount.getAccount() + " : Login Fail(" + params[2] + ").");
                        break;
                    }
                    continue;
                }

                // Call state
                if(params[0].equals("CALLSTATE")){
                    //System.out.println(data);
                    onCallState(params);
                }

                // Hang up
                if(params[0].equals("HANGUP")){
                    //System.out.println(data);
                    onHangUp(params);
                }
            }
            System.out.println("[Account Status] " + pbxAccount.getAccount() + " : Terminated.");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String makeLoginPacket(){
        String packetStart = "SP";
        String packetData;
        String packetLen;
        String packetEnd = "M";
        String id = pbxAccount.getAccount();
        String password = pbxAccount.getPassword();
        String extenNumber = pbxAccount.getExtenNumber();
        String saltedPassword;
        String packet ="";

        // password SHA-512
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(pbxAccount.getPassword().getBytes());
            saltedPassword = String.format("%0128x", new BigInteger(1, md.digest()));
            // make packet

            packetData = "LOGIN_REQ&" + id + "&" + saltedPassword + "&" + extenNumber;
            packetLen = Integer.toString(packetData.length());
            while(packetLen.length() != 3){
                packetLen = "0" + packetLen;
            }
            packet += packetStart;
            packet += packetLen;
            packet += packetData;
            packet += packetEnd;
            // ------

            // Login packet Print
            //System.out.println(packet);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return packet;
    }

    private void onCallState(String[] params){
        String type = params[1];
        String typeDescription = "";
        String inNumber = params[2];
        String srcNumber = params[3];
        String dstNumber = params[4];
        String time = getCurrentTime();

        switch (type){
            case "IR":
                typeDescription = "통화 수신";
                onCallStateIR(inNumber,srcNumber,dstNumber);
                break;
            case "IA":
                typeDescription = "수신 통화 중";
                onCallStateIA(inNumber,srcNumber,dstNumber);
                break;
            case "OR":
                typeDescription = "통화 발신";
                break;
            case "OA":
                typeDescription = "발신 통화 중";
                break;
            case "PU":
                typeDescription = "당겨 받기";
                break;
        }

        System.out.println("[" + time + "] " + pbxAccount.getAccount() + " : " + typeDescription + " : " +srcNumber + " -> " + dstNumber + "(" + inNumber + ")");
    }

    private void onCallStateIR(String inNumber, String srcNumber, String dstNumber){
        // if call is being processed, do not update database
        if(pbxAccount.getCurrentAccountCall() != null) return;

        PbxAccountCall pbxAccountCall = new PbxAccountCall(pbxAccount.getId(),"in",dstNumber,srcNumber);
        pbxAccount.addPbxAccountCall(pbxAccountCall);
        pbxAccountCall.insertPbxAccountCall();

        String jsonValue = "{ \"pbx_account_call_id\":\""+pbxAccountCall.getId().toString()+"\" }";
        //System.out.println(jsonValue);
        postUUID(jsonValue);

    }
    // answer
    private void onCallStateIA(String inNumber, String srcNumber, String dstNumber){
        String identifier = srcNumber + dstNumber;
        PbxAccountCall pbxAccountCall = pbxAccount.findPbxAccountCall(identifier);
        if(pbxAccountCall == null){
            System.out.println("No Mapped Call..");
        }
        else{
            // update 'answerAt column'
            pbxAccountCall.updatePbxAccountCallAnswerAt();
            String jsonValue = "{ \"pbx_account_call_id\":\""+pbxAccountCall.getId().toString()+"\" }";
            //System.out.println(jsonValue);
            postUUID(jsonValue);
        }
    }

    private void postUUID(String jsonValue){
        try {
            URL url = new URL(cmsPostAddress);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");

            OutputStream os = connection.getOutputStream();
            os.write(jsonValue.getBytes());
            os.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuffer oneResult = new StringBuffer();
            String inputLine = null;
            while((inputLine = in.readLine()) != null){
                oneResult.append(inputLine);
            }

            //System.out.println(inputLine);

            connection.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onHangUp(String[] params){
        String type = params[1];
        String typeDescription = "";
        String inNumber = params[2];
        String srcNumber = params[3];
        String dstNumber = params[4];
        String time = getCurrentTime();

        switch (type){
            case "IR":
                typeDescription = "통화 수신 중에 끊김";
                updateHangUpStatus(srcNumber,dstNumber);
                break;
            case "IA":
                typeDescription = "통화 중에 끊김";
                updateHangUpStatus(srcNumber,dstNumber);
                break;
            case "OR":
                typeDescription = "통화 발신";
                break;
            case "OA":
                typeDescription = "발신 통화 중";
                break;
            case "PU":
                typeDescription = "당겨 받기";
                break;
        }

        System.out.println("[" + time + "] " + pbxAccount.getAccount() + " : " + typeDescription + " : " +srcNumber + " -> " + dstNumber + "(" + inNumber + ")");
    }

    private void updateHangUpStatus(String srcNumber, String dstNumber){
        String identifier = srcNumber + dstNumber;
        PbxAccountCall pbxAccountCall = pbxAccount.findPbxAccountCall(identifier);
        if(pbxAccountCall == null){
            System.out.println("No Mapped Call..");
        }
        else{
            // update 'answerAt column'
            pbxAccountCall.updatePbxAccountCallHangUpAt();
            String jsonValue = "{ \"pbx_account_call_id\":\""+pbxAccountCall.getId().toString()+"\" }";
            //System.out.println(jsonValue);
            postUUID(jsonValue);
            pbxAccount.setCurrentAccountCall(null);
        }
    }

    public void start(){
        receiveDataThread = new ReceiveDataThread(this);
        Thread t = new Thread(receiveDataThread);
        t.start();
    }

    public PbxAccount getPbxAccount() {
        return pbxAccount;
    }

    public static void main(String[] args){
        System.out.println("Main Start");

        PbxAccounts pbxAccounts = new PbxAccounts();
        List<PbxAccount> pbxAccountList = pbxAccounts.getPbxAccountList();

        for(PbxAccount pbxAccount : pbxAccountList){
            PBXSocketClient pbxSocketClient = new PBXSocketClient(pbxAccount);
            pbxSocketClient.start();
        }
    }

}
