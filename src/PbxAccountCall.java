import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.SimpleFormatter;

public class PbxAccountCall {
    private UUID id;
    private UUID pbx_account_id;
    private final String vendor_id = "vizufon";
    private final String vendor_name = "vizufon";
    private String bound_type;
    private String incoming_number;
    private String outgoing_number;
    private String general_directory_number = "";
    private String answer_at;
    private String hangup_at;
    private String history_at;
    private String created_at;
    private String identifier; // srcNumber + dstNumber
    PbxQueryFactory pbxQueryFactory;

    //virtual number call id
    private UUID vid;
    private String tel_management_id;

    //virtual number call mapping id
    private UUID vmid;


    public PbxAccountCall(UUID pbx_account_id, String bound_type, String incoming_number, String outgoing_number) {
        this.id = UUID.randomUUID();
        this.pbx_account_id = pbx_account_id;
        this.bound_type = bound_type;
        this.incoming_number = incoming_number;
        this.outgoing_number = outgoing_number;
        this.history_at = getConvertedDateTime();
        this.identifier = outgoing_number + incoming_number; // call 식별자

        pbxQueryFactory = new PbxQueryFactory();

        ResultSet rs = pbxQueryFactory.executeSelectTelManagement(outgoing_number);

        while(true) {
            try {
                if (!rs.next()) break;
                tel_management_id = rs.getString("id");
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        this.vid = UUID.randomUUID();
        this.vmid = UUID.randomUUID();
    }

    public PbxAccountCall(UUID id, UUID pbx_account_id, String bound_type, String incoming_number, String outgoing_number, String history_at) {
        this.id = id;
        this.pbx_account_id = pbx_account_id;
        this.bound_type = bound_type;
        this.incoming_number = incoming_number;
        this.outgoing_number = outgoing_number;
        this.history_at = history_at;
        this.identifier = outgoing_number + incoming_number; // call 식별자

        pbxQueryFactory = new PbxQueryFactory();

        ResultSet rs = pbxQueryFactory.executeSelectTelManagement(outgoing_number);

        while(true) {
            try {
                if (!rs.next()) break;
                tel_management_id = rs.getString("id");
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            this.vid = UUID.randomUUID();
        }
        this.vmid = UUID.randomUUID();
    }

    private String getConvertedDateTime(){
        Instant instant = Instant.now();
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        ZonedDateTime zonedDateTime = instant.atZone(seoul);
        return zonedDateTime.toString().replace('T', ' ').substring(0,19);
    }

    public void insertPbxAccountCall(){
        if(tel_management_id == null) return;
        pbxQueryFactory.executeInsertCallData(this);
    }

    public void updatePbxAccountCallAnswerAt(){
        pbxQueryFactory.executeUpdateAnswerAt(this);
    }

    public void updatePbxAccountCallHangUpAt(){
        pbxQueryFactory.executeUpdateHangUpAt(this);
    }

    // 미완성
    public boolean isSameCall(String identifier){
        if(identifier.equals(identifier))
            return true;
        else
            return false;
    }

    public String getIncoming_number() {
        return incoming_number;
    }

    public String getOutgoing_number() {
        return outgoing_number;
    }

    public String getHistory_at() {
        return history_at;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPbx_account_id() {
        return pbx_account_id;
    }

    public void setPbx_account_id(UUID pbx_account_id) {
        this.pbx_account_id = pbx_account_id;
    }

    public String getVendor_id() {
        return vendor_id;
    }

    public String getVendor_name() {
        return vendor_name;
    }

    public String getBound_type() {
        return bound_type;
    }

    public void setBound_type(String bound_type) {
        this.bound_type = bound_type;
    }

    public void setIncoming_number(String incoming_number) {
        this.incoming_number = incoming_number;
    }

    public void setAnswer_at(String answer_at) {
        this.answer_at = answer_at;
    }

    public void setHangup_at(String hangup_at) {
        this.hangup_at = hangup_at;
    }

    public void setHistory_at(String history_at) {
        this.history_at = history_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public void setOutgoing_number(String outgoing_number) {
        this.outgoing_number = outgoing_number;
    }

    public String getGeneral_directory_number() {
        return general_directory_number;
    }

    public void setGeneral_directory_number(String general_directory_number) {
        this.general_directory_number = general_directory_number;
    }

    public UUID getVid() {
        return vid;
    }

    public String getTel_management_id() {
        return tel_management_id;
    }

    public UUID getVmid() {
        return vmid;
    }
}
