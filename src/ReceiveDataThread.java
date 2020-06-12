
import java.io.*;
import java.net.Socket;

public class ReceiveDataThread implements Runnable{
    PBXSocketClient psc;

    public ReceiveDataThread(PBXSocketClient psc){
        this.psc = psc;
    }

    public void run(){
        System.out.println("[Account Thread Status] " + psc.getPbxAccount().getExtenNumber() + " : Run Thread.");
        psc.run();
    }
}
