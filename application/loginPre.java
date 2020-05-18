package application;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JOptionPane;

public class loginPre extends Application {
    private Stage primaryStage;
    private TextField usernameTF; 
    private PasswordField pwTF;
    private Button loginBtn;
	final String dbUrl = "jdbc:ucanaccess://src//SchedulerDB.accdb";

    @Override
    public void start(Stage ps) throws Exception {
        primaryStage = ps;
        primaryStage.setTitle("Patient Scheduling System");
        primaryStage.setScene(createLoginScene());
        primaryStage.show();

    }

    public Scene createLoginScene(){
        Label enterLoginInfoLb = new Label("Enter Login Credentials");
        Label userLb = new Label("Username: ");
        Label pwLb = new Label("Password: ");
       
        ChoiceBox<String> accountTypeDrop = new ChoiceBox<>();
        accountTypeDrop.getItems().addAll("Receptionist", "Doctor", "Medical Employee");
        
        LoginTextFieldListener textFieldListener = new LoginTextFieldListener();
        usernameTF = new TextField();
        pwTF = new PasswordField();
        usernameTF.textProperty().addListener(textFieldListener);
        pwTF.textProperty().addListener(textFieldListener);
        loginBtn = new Button("Login");

        GridPane gpLogin = new GridPane();
        gpLogin.addRow(0, userLb, usernameTF);
        gpLogin.addRow(1, pwLb, pwTF);
        gpLogin.setVgap(10);
        gpLogin.setHgap(10);
        gpLogin.setAlignment(Pos.CENTER);
        VBox vbLogin = new VBox(20, enterLoginInfoLb, gpLogin, accountTypeDrop, loginBtn);
        vbLogin.setAlignment(Pos.CENTER);
        loginBtn.setOnAction(e -> handle(accountTypeDrop));
        return new Scene(vbLogin, 475, 375);
    }
    
    /*
     * Patient schedule scene displays the weekly schedule.... this is for testing
     */
    public Scene scheduleDisplay () {	
    	Label patientSchedule = new Label("Patient Scheduler"); //only for testing, should be changed depending on different users.
    	VBox layout = new VBox(20, patientSchedule);
    	layout.setAlignment(Pos.CENTER);
    	return new Scene (layout, 600, 500);
    }
    
    

    public class LoginTextFieldListener implements ChangeListener<String>{
        @Override
        public void changed(ObservableValue<? extends String> source, String oldValue, String newValue) {
            String usernameVal = usernameTF.getText();
            String passwordVal = pwTF.getText();
            loginBtn.setDisable(usernameVal.trim().equals("") || passwordVal.trim().equals(""));
        }
    }
    
    /*
     * @param takes in the drop down menu option
     * Desc: when user presses login button, the application will make connection to database to confirm credentials
     */
    public void handle (ChoiceBox<String> accountTypeDrop ) { // when the button is clicked
    		try { 
    			String credential = accountTypeDrop.getValue();	// getting the option the user chose in dropdown
    			String IDquery = null;
    			String passQuery = null;
    			if(credential=="Receptionist") {				// changes values for obtaining login from msaccess database
    				IDquery = "Receptionist_ID=?";
    				passQuery = "R_Password=?";
    			}
    			else if (credential=="Doctor") {
    				IDquery = "Doctor_ID=?";
    				passQuery = "D_Password=?";
    			}
    			else if (credential=="Medical Employee") {
    				credential = "MedicalEmployee";
    				IDquery = "Med_Employee_ID=?";
    				passQuery = "ME_Password=?";
    			}
    			int ID = Integer.parseInt(usernameTF.getText());  // get the ID number from the input
    			String pword = pwTF.getText();
    			Connection con = DriverManager.getConnection(dbUrl);
				PreparedStatement pst = con.prepareStatement("Select * FROM " +credential+ " WHERE " +IDquery+ " AND " +passQuery);
				pst.setInt(1, ID);
				pst.setString(2,pword);
				ResultSet rs = pst.executeQuery();
				if (rs.next()) {			
					/*
					 * ADD if statements to open specific schedules for different users; for example if it is doctor, open the 
					 * schedule so that it is the correct schedule for the role of the doctor to view. Doctors only get 
					 * to view the schedule. EX: primaryStage.setScene(scheduleDisplayDoctor)
					 */
					primaryStage.setScene(scheduleDisplay());
				} 
				else {
					Alert wrongCred = new Alert(AlertType.ERROR);
					wrongCred.setHeaderText("Invalid username or password");
					wrongCred.setContentText("Please contact your administrator if you forgot your credentials");
					wrongCred.showAndWait();
				}
    		} catch (Exception e) {
    			System.out.println("Connection to database failed");
    			Alert invalidUser = new Alert(AlertType.ERROR);
				invalidUser.setHeaderText("Invalid username or password");
				invalidUser.setContentText("Please contact your administrator if you forgot your credentials");
				invalidUser.showAndWait();
    		}
    }
    /**
     * @wbp.parser.entryPoint
     */
    public static void main(String[] args) {
        launch(args);
    }
}