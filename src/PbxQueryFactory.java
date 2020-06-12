import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PbxQueryFactory {
    private final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private final String URL = "mysql jdbc url";

    private final String USER_NAME = "username";
    private final String PASSWORD = "password";

    Connection conn = null;
    Statement statement = null;

    public PbxQueryFactory(){
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
            //System.out.println("[MySQL Connection] ");
            statement = conn.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet executeSelect(String what, String targetTable, String condition){
        String sql;
        ResultSet resultSet;
        sql = "SELECT " + what +
                " FROM " + targetTable;
        //System.out.println(sql);
        if(condition.length() != 0){
            sql += " WHERE " + condition;
        }

        try {
            resultSet = statement.executeQuery(sql);
            return resultSet;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return null;
    }

    public ResultSet executeSelectTelManagement(String virtualNumber){
        String what = "id";
        String targetTable = "tel_management";
        String condition = "tel='" + virtualNumber +"'";

        return executeSelect(what, targetTable, condition);
    }

    public ResultSet executeSelectRecentPbxAccountCall(PbxAccount pbxAccount){
        String what = "bin_to_uuid(id) as id, bin_to_uuid(pbx_account_id) as pbx_account_id, bound_type, incoming_number, outgoing_number, answer_at, hang_up_at, history_at";
        String targetTable = "pbx_account_calls";
        String condition = "pbx_account_id=uuid_to_bin('" + pbxAccount.getId() + "') " +
                "ORDER BY created_at desc " +
                "limit 1";

        return executeSelect(what,targetTable,condition);
    }

    public void executeInsert(String targetTable, String values){
        String sql;
        sql = "INSERT INTO " + targetTable + " VALUES " + values;
        //System.out.println(sql);

        try {
            statement.execute(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void executeInsertCallData(PbxAccountCall pbxAccountCall){
        executeInsertPbxAccountCalls(pbxAccountCall);
        executeInsertVirtualNumberCall(pbxAccountCall);
        executeInsertPbxAccountCallMappingVirtualNumberCall(pbxAccountCall);
    }

    public void executeInsertPbxAccountCalls(PbxAccountCall pbxAccountCall){
        String id = "'" + pbxAccountCall.getId().toString() + "'";
        String pbxId = "'" + pbxAccountCall.getPbx_account_id().toString() + "'";
        String vendorId = "'" + pbxAccountCall.getVendor_id() + "'";
        String vendorName = "'" + pbxAccountCall.getVendor_name() + "'";
        String boundType = "'" + pbxAccountCall.getBound_type() + "'";
        String incomingNumber = "'" + pbxAccountCall.getIncoming_number() + "'";
        String outgoingNumber = "'" + pbxAccountCall.getOutgoing_number() + "'";
        //String generalDirectoryNumber = pbxAccountCall.getGeneral_directory_number();
        String historyAt = "'" + pbxAccountCall.getHistory_at()  + "'";

        String targetTable = "pbx_account_calls(id, pbx_account_id, vendor_id, vendor_name" +
                ", bound_type, incoming_number, outgoing_number, history_at)";
        String values = "(uuid_to_bin(" + id + "),uuid_to_bin(" + pbxId + ")," +vendorId + "," +vendorName + "," +boundType + "," +incomingNumber + "," +outgoingNumber + "," + historyAt + ")";

        executeInsert(targetTable,values);

    }

    public void executeInsertVirtualNumberCall(PbxAccountCall pbxAccountCall){
        String id = "'" + pbxAccountCall.getVid().toString() + "'";
        String telManagementId = pbxAccountCall.getTel_management_id();
        String virtualNumber = "'" + pbxAccountCall.getIncoming_number() + "'";
        String incomingNumber = "'" + pbxAccountCall.getIncoming_number() + "'";
        String outgoingNumber = "'" + pbxAccountCall.getOutgoing_number() + "'";
        String historyAt = "'" + pbxAccountCall.getHistory_at() + "'";

        String targetTable = "virtual_number_calls(id,tel_management_id,virtual_number,incoming_number,outgoing_number,history_at)";
        String values = "(uuid_to_bin(" + id + ")," + telManagementId + "," + virtualNumber + "," +incomingNumber+","+outgoingNumber+","+historyAt+")";

        executeInsert(targetTable,values);
    }

    public void executeInsertPbxAccountCallMappingVirtualNumberCall(PbxAccountCall pbxAccountCall){
        String id = "'" + pbxAccountCall.getVmid().toString() + "'";
        String pbxId = "'" + pbxAccountCall.getId().toString() + "'";
        String vid = "'" + pbxAccountCall.getVid().toString() + "'";

        String targetTable = "pbx_account_call_mapping_virtual_number_call(id, pbx_account_call_id, virtual_number_call_id)";
        String values =  "(uuid_to_bin(" + id + ")," +  "uuid_to_bin(" + pbxId + ")," +  "uuid_to_bin(" + vid + "))";

        executeInsert(targetTable,values);
    }

    public void executeUpdate(String targetTable, String op, String condition){
        String sql;
        sql = "UPDATE " + targetTable + " SET " + op + " WHERE " + condition;
        //System.out.println(sql);
        try {
            statement.execute(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void executeUpdateAnswerAt(PbxAccountCall pbxAccountCall){
        String id = "'" + pbxAccountCall.getId().toString() + "'";
        String targetTable = "pbx_account_calls";
        String op = "answer_at=" + "'" + getConvertedDateTime() + "'";
        String condition = "id=uuid_to_bin(" + id + ")";

        executeUpdate(targetTable, op, condition);
    }

    public void executeUpdateHangUpAt(PbxAccountCall pbxAccountCall){
        String id = "'" + pbxAccountCall.getId().toString() + "'";
        String targetTable = "pbx_account_calls";
        String op = "hang_up_at=" + "'" + getConvertedDateTime() + "'";
        String condition = "id=uuid_to_bin(" + id + ")";

        executeUpdate(targetTable, op, condition);
    }

    private String getConvertedDateTime(){
        Instant instant = Instant.now();
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        ZonedDateTime zonedDateTime = instant.atZone(seoul);
        return zonedDateTime.toString().replace('T', ' ').substring(0,19);
    }

}
