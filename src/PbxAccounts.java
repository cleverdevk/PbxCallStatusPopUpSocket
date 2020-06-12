import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PbxAccounts {

    public List<PbxAccount> getPbxAccountList() {
        List<PbxAccount> pbxAccountList = new ArrayList<>();
        PbxQueryFactory pbxQueryFactory = new PbxQueryFactory();
        ResultSet resultSet = pbxQueryFactory.executeSelect("bin_to_uuid(id) as id, account, password, extension_number"
                                                            , "pbx_accounts", "");

        try {
            while (resultSet.next()) {
                String account = resultSet.getString("account");
                String password = resultSet.getString("password");
                String extenNumber = resultSet.getString("extension_number");
                UUID id = UUID.fromString(resultSet.getString("id"));
                //System.out.println(resultSet.getString("id") + " " + account + " " + password + " " + extenNumber);
                pbxAccountList.add(new PbxAccount(id, account, password, extenNumber));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return pbxAccountList;
    }
}
