import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PbxAccount {
    private String account;
    private String password;
    private String extenNumber;
    private Instant createdAt;
    private UUID id;
    private PbxQueryFactory pbxQueryFactory;

    //private List<PbxAccountCall> pbxAccountCallList;
    private PbxAccountCall currentAccountCall = null;

    public PbxAccount(UUID id, String account, String password, String extenNumber){
        this.account = account;
        this.password = password;
        this.extenNumber = extenNumber;
        this.id = id;
        this.pbxQueryFactory = new PbxQueryFactory();

        Instant instant = Instant.now();
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        ZonedDateTime zonedDateTime = instant.atZone(seoul);

        createdAt = zonedDateTime.toInstant();
        //pbxAccountCallList = new ArrayList<>();
        ResultSet rs = pbxQueryFactory.executeSelectRecentPbxAccountCall(this);

        while(true){
            try {
                if (!rs.next()) {
                    System.out.println("[Previous Call Status] " + this.account + "(" + this.extenNumber + ") : waiting.");
                    break;
                }

                UUID cid = UUID.fromString(rs.getString("id"));
                UUID pid = UUID.fromString(rs.getString("pbx_account_id"));
                String bound_type = rs.getString("bound_type");
                String incoming_number = rs.getString("incoming_number");
                String outgoing_number = rs.getString("outgoing_number");
                String history_at = rs.getString("history_at");
                String hang_up_at = rs.getString("hang_up_at");
                //System.out.println(incoming_number);

                if (hang_up_at != null) {
                    System.out.println("[Previous Call Status] " + this.account + "(" + this.extenNumber + ") : waiting.");
                    break;
                }
                else {
                    System.out.println("[Previous Call Status] " + this.account + "(" + this.extenNumber + ") : There is the call on process.");
                    currentAccountCall = new PbxAccountCall(cid,pid,bound_type,incoming_number,outgoing_number,history_at);
                    break;
                }


            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExtenNumber() {
        return extenNumber;
    }

    public void setExtenNumber(String extenNumber) {
        this.extenNumber = extenNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }
    public void addPbxAccountCall(PbxAccountCall pbxAccountCall){
        //pbxAccountCallList.add(pbxAccountCall);
        currentAccountCall = pbxAccountCall;
    }

    public PbxAccountCall findPbxAccountCall(String identifier){
        //for(PbxAccountCall pbxAccountCall : pbxAccountCallList){
        //    if(pbxAccountCall.isSameCall(identifier))
        //        return pbxAccountCall;
        //}
        //return null;
        if(currentAccountCall == null) return null;
        if(currentAccountCall.isSameCall(identifier)) return currentAccountCall;
        else return null;
    }

    public PbxAccountCall getCurrentAccountCall() {
        //pbxAccountCallList = new ArrayList<>();
        ResultSet rs = pbxQueryFactory.executeSelectRecentPbxAccountCall(this);

        while(true){
            try {
                if (!rs.next()) {
                    System.out.println("[Update Call Status] " + this.extenNumber + " : waiting.");
                    break;
                }

                UUID cid = UUID.fromString(rs.getString("id"));
                UUID pid = UUID.fromString(rs.getString("pbx_account_id"));
                String bound_type = rs.getString("bound_type");
                String incoming_number = rs.getString("incoming_number");
                String outgoing_number = rs.getString("outgoing_number");
                String history_at = rs.getString("history_at");
                String hang_up_at = rs.getString("hang_up_at");
                //System.out.println(incoming_number);

                if (hang_up_at != null) {
                    System.out.println("[Update Call Status] " + this.extenNumber + " : waiting.");
                    currentAccountCall = null;
                    break;
                }
                else {
                    System.out.println("[Update Call Status] " + this.extenNumber + " : There is the call on process.");
                    currentAccountCall = new PbxAccountCall(cid,pid,bound_type,incoming_number,outgoing_number,history_at);
                    break;
                }


            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }
        return currentAccountCall;
    }

    public void setCurrentAccountCall(PbxAccountCall currentAccountCall) {
        this.currentAccountCall = currentAccountCall;
    }
}
