/*	SecureChat: A secure chat system which permits secure communications 
 *  between iOS devices and a back-end server.
 *
 *	Copyright © 2016 by William Edward Woody
 *
 *	This program is free software: you can redistribute it and/or modify it 
 *	under the terms of the GNU General Public License as published by the 
 *	Free Software Foundation, either version 3 of the License, or (at your 
 *	option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, but 
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *	for more details.
 *
 *	You should have received a copy of the GNU General Public License along 
 *	with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.chaosinmotion.caredemo.server.database;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import com.chaosinmotion.caredemo.server.config.Config;
import com.chaosinmotion.caredemo.shared.Constants;
import com.chaosinmotion.caredemo.shared.SHA256Hash;

/**
 * This contains the code which builds the initial database 
 */
public class DatabaseBuilder
{
    private static boolean gInit = false;
    
    public synchronized static Connection openConnection() throws ClassNotFoundException, IOException, SQLException
    {
        Class.forName("org.postgresql.Driver"); 

        Properties p = Config.get();
        String url = p.getProperty("dburl");
        String uname = p.getProperty("username");
        String pword = p.getProperty("password");
        Connection conn = DriverManager.getConnection(url,uname,pword);
        
        if (!gInit) {
            gInit = true;
            
            /*
             * Start with bootstrap of version file
             */
            
            int version = 0;
            boolean exists = true;
            try {
                PreparedStatement ps = conn.prepareStatement("SELECT MAX(version) FROM DBVERSION");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    version = rs.getInt(1);
                }
                rs.close();
                ps.close();
            }
            catch (SQLException ex) {
                exists = false;
            }
            
            if (!exists) {
                runSchemaFile(conn,"coreschema.sql");
            }
            runUpdateSchema(version,conn);
            
            /*
             * Bootstrap a user if the users table is empty.
             */
            
            try {
                PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Users");
                ResultSet rs = ps.executeQuery();
                int count = 0;
                if (rs.next()) {
                	count = rs.getInt(1);
                }
                rs.close();
                ps.close();
                
                if (count == 0) {
                	/*
                	 *  If there are no users, we insert an administrator to
                	 *  bootstrap our system
                	 */
                	ps = conn.prepareStatement("INSERT INTO Users "
                			+ "( username, email, name, password ) "
                			+ "VALUES "
                			+ "( ?, ?, ?, ? ); SELECT currval('Users_userid_seq')");
                	ps.setString(1, "admin");
                	ps.setString(2, "");
                	ps.setString(3, "Admin");
                	ps.setString(4, SHA256Hash.hash("password" + Constants.SALT));
                	
                	ps.execute();
                	
                    int utc = ps.getUpdateCount();
                    int index = 0;
                    if ((utc == 1) && ps.getMoreResults()) {
                        rs = ps.getResultSet();
                        if (rs.next()) {
                        	index = rs.getInt(1);
                        }
                        rs.close();
                    }

                    /*
                	 * Now set the admin ACL
                	 */
                	
                	ps.close();
                	ps = conn.prepareStatement("INSERT INTO UserAccessControlList " +
                			"    ( userid, ace ) " +
                			"VALUES " +
                			"    ( ?, ? )");
                	ps.setInt(1, index);
                	ps.setInt(2, 1);			// ACE for Administrator
                	ps.execute();
                }
            }
            catch (SQLException ex) {
            }
        }
        
        return conn;
    }
   
    /**
     * This opens a statement object and starts executing the statements in the
     * local object named in the name given.
     * @param conn
     * @param name
     * @return False if the resource file doesn't exist
     * @throws SQLException
     * @throws IOException
     */
    private static boolean runSchemaFile(Connection conn, String name) throws SQLException, IOException
    {
        InputStream is = DatabaseBuilder.class.getResourceAsStream(name);
        if (is == null) return false;
        SQLReader reader = new SQLReader(new InputStreamReader(is));

        Statement s = conn.createStatement();
        String statement;
        while (null != (statement = reader.getStatement())) {
//          System.out.println("Statement: " + statement);
            try {
                s.executeUpdate(statement);
            }
            catch (SQLException ex) {
                System.err.println("While executing:");
                System.err.println(statement);
                throw ex;
            }
        }
        is.close();
        s.close();
        return true;
    }
    
    /**
     * This loads the version stored in the DBVERSION table, and executes the various
     * SQL files as needed to bring the version to the latest version
     * @param conn
     * @throws SQLException
     * @throws IOException
     * @throws BLDatabaseException 
     */
    private static void runUpdateSchema(int version, Connection conn) throws SQLException, IOException
    {
        /*
         * Get the current version in the database
         */
        Statement s = conn.createStatement();
        
        /*
         * The current version. Start loading schema files in the form schemaN.sql
         */
        int i = version+1;
        while (runSchemaFile(conn,"schema" + i + ".sql")) {
            s.executeUpdate("INSERT INTO DBVERSION ( version ) VALUES ( " + i + " )");
            ++i;
        }
        s.close();
    }
}


