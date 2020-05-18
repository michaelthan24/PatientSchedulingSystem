package application;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class Main extends Application {
    private Button loginBtn, logoutBtn, addBtn, editBtn, delBtn, nextDayBtn, prevDayBtn, exitBtn, scheduleBtn, cancelBtn, viewProfileBtn, createRecBtn, reminderBtn;
    private TextField usernameTF, pwTF, currentPW, searchRecordTF;
    private Stage primaryStage, addAppointmentWindow, userProfileWindow, editPatientRecordWindow, deleteRecordWindow, addNonApptWindow;
    private String sessionType, sessionUserID, currentView, selectedRecordID;
    private BorderPane home, functions, data;
    private RadioButton dayView, patientDBView, employeeDBView, doctorDBView, appointmentDBView;
    private ToggleGroup viewOptions, dayViewAppointments, dataBaseSelection;
    private LocalDate today, currentDay;
    private Label date;
    private Scheduler dbAccessor;
    private Scheduler.Patient selectedPatRec;
    private Scheduler.Employee selectedEmpRec;
    private Scheduler.Doctor selectedDocRec;
    private Scheduler.Appointment selectedAppt;
    private static TimeSlot selected;
    final Color startColor = Color.web("#e08090");
    final Color endColor = Color.web("#80e090");
    final ObjectProperty<Color> color = new SimpleObjectProperty<Color>(startColor);
    final StringBinding flash = Bindings.createStringBinding(new Callable<String>() {
        @Override
        public String call() throws Exception {
            return String.format("-fx-body-color: rgb(%d, %d, %d);",
                    (int) (256*color.get().getRed()),
                    (int) (256*color.get().getGreen()),
                    (int) (256*color.get().getBlue()));
        }
    }, color);

    final Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(color, startColor)),
            new KeyFrame(Duration.seconds(1), new KeyValue(color, endColor)));

    @Override
    public void start(Stage ps) throws Exception {
        primaryStage = ps;
        primaryStage.setTitle("Patient Scheduling System");
        primaryStage.setResizable(false);
        primaryStage.setScene(createLoginScene());
        primaryStage.show();
    }

    public Scene createHomeScene(){
        dbAccessor = new Scheduler();
        home = new BorderPane();
        today = LocalDate.now();
        currentDay = today;
        currentView = "Day View";
        reminderBtn = new Button("Appt. Reminders");
        reminderBtn.setPrefWidth(200);
        reminderBtn.styleProperty().bind(flash);
        reminderBtn.setOnAction(reminderListEvent);
        setViewOptions();
        setDataOptions();
        home.setLeft(functions);
        setData();
        home.setRight(data);
        return new Scene(home, 800, 600);
    }

    public void setData(){
        data = new BorderPane();
        data.setPrefSize(600,600);
        if(currentView == "Day View"){
            //top: Day View header + current date
            Label header = new Label("Day View: ");
            header.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 24));
            date = new Label(currentDay.getMonth() + " " + currentDay.getDayOfMonth() + ", " + currentDay.getYear());
            date.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 18));
            HBox dayViewHeader = new HBox(header, date);
            dayViewHeader.setPadding(new Insets(5,0,5,10));
            dayViewHeader.setAlignment(Pos.CENTER_LEFT);
            dayViewHeader.setPrefSize(600, 50);
            dayViewHeader.setStyle("-fx-background-color: #f5fffa");
            data.setTop(dayViewHeader);
            data.setCenter(createDayView());
        }else{
            //database view
            data.setCenter(createDatabaseView());
        }
        home.setRight(data);
    }

    public BorderPane createDatabaseView(){
        BorderPane recordData = new BorderPane();
        recordData.setPrefSize(600, 600);
        //set header and set recordData.Center
        Label header;
        if(currentView == "Patient"){
            header = new Label("Patient Records");
            header.setPadding(new Insets(5,5,5,5));
            Scheduler.Patient[] patientList = dbAccessor.getPatientRecords().toArray(new Scheduler.Patient[dbAccessor.getPatientRecords().size()]);
            recordData.setCenter(buildRecordsData(patientList, "Patient"));
        }else if(currentView == "Employee"){
            header = new Label("Employee Records");
            Scheduler.Employee[] employeeList = dbAccessor.getEmployeeRecords().toArray(new Scheduler.Employee[dbAccessor.getEmployeeRecords().size()]);
            recordData.setCenter(buildRecordsData(employeeList, "Employee"));
        }else if(currentView == "Doctor"){
            header = new Label("Doctor Records");
            Scheduler.Doctor[] doctorList = dbAccessor.getDoctorRecords().toArray(new Scheduler.Doctor[dbAccessor.getDoctorRecords().size()]);
            recordData.setCenter(buildRecordsData(doctorList, "Doctor"));
        }else{
            header = new Label("Appointment Records: Next 3 Days");
            Scheduler.Appointment[] apptList;
            ArrayList<Scheduler.Appointment> appointments;
            String type;
            if(sessionType == "Doctor"){
                appointments = dbAccessor.getApptsForRV(today, Integer.parseInt(sessionUserID));
                appointments.addAll(dbAccessor.getApptsForRV(today.plusDays(1),Integer.parseInt(sessionUserID)));
                appointments.addAll(dbAccessor.getApptsForRV(today.plusDays(2),Integer.parseInt(sessionUserID)));
                type = "Your Appointments";
            }else{
                appointments = dbAccessor.getApptsForRV(today);
                appointments.addAll(dbAccessor.getApptsForRV(today.plusDays(1)));
                appointments.addAll(dbAccessor.getApptsForRV(today.plusDays(2)));
                type = "Appointment";
            }
            apptList = appointments.toArray(new Scheduler.Appointment[appointments.size()]);
            recordData.setCenter(buildRecordsData(apptList, type));

        }
        header.setPrefSize(600, 50);
        header.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        header.setStyle("-fx-background-color: #d3d3d3");
        header.setAlignment(Pos.CENTER_LEFT);
        recordData.setTop(header);
        //bottom: search by RecordID
        Label search = new Label("Find by ID: ");
        search.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 16));
        searchRecordTF = new TextField("Enter ID");
        Button searchRecordBtn = new Button("Search");
        searchRecordBtn.setPrefSize(100, 30);
        searchRecordBtn.setDisable(true);
        searchRecordTF.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                searchRecordBtn.setDisable(false);
            }
        });
        HBox searchBar = new HBox();
        searchBar.getChildren().addAll(search, searchRecordTF, searchRecordBtn);
        searchBar.setSpacing(4);
        searchBar.setPadding(new Insets(0, 0,0,10));
        searchBar.setAlignment(Pos.CENTER_LEFT);
        //recordData.setBottom(searchBar);
        return recordData;
    }

    public ScrollPane buildRecordsData(Object[] list, String recordType){
        if(list[0] == null){
            ScrollPane noContent = new ScrollPane();
            Label empty = new Label("No Records in Database");
            empty.setPrefSize(300,75);
            empty.setAlignment(Pos.CENTER);
            empty.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            noContent.setContent(empty);
            return noContent;
        }
        String[] fields;
        if(recordType == "Patient"){
            fields = new String[]{"ID", "First Name", "Last Name", "DOB", "SSN", "Phone", "Address", "Email"};
        }else if(recordType == "Appointment"){
            fields = new String[]{"ApptID", "Patient", "Date", "Time", "Doctor", "Reason"};
        }else if(recordType == "Employee"){
            fields = new String[]{"EmployeeID", "Name", "Password", "EmployeeType"};
        }else if(recordType == "Your Appointments") {
            fields = new String[]{"ApptID", "Patient", "Date", "Time", "Doctor", "Reason"};
        }else{
            fields = new String[]{"DoctorID", "Name", "Phone", "Password"};
        }
        GridPane records = new GridPane();
        records.setPrefHeight(550);
        records.setGridLinesVisible(true);
        dataBaseSelection = new ToggleGroup();
        dataBaseSelection.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
                if(newValue == null){
                    addBtn.setDisable(false);
                    editBtn.setDisable(true);
                    delBtn.setDisable(true);
                }else{
                    selectedRecordID = newValue.getUserData().toString();
                    if(recordType == "Patient"){
                        addBtn.setDisable(true);
                        editBtn.setDisable(false);
                        editBtn.setOnAction(editPatientRecordEvent);
                        delBtn.setDisable(false);
                        delBtn.setOnAction(delRecordEvent);
                    }else{
                        addBtn.setDisable(true);
                        delBtn.setDisable(false);
                        delBtn.setOnAction(delRecordEvent);
                    }
                }
            }
        });
        //first row: field headers, get class fields of list[i]
        Label[] val = new Label[fields.length+1];
        Label spaceForRadioButtons = new Label(" ");
        val[0] = spaceForRadioButtons;
        for(int i=0;i<fields.length;i++){
            Label temp = new Label(fields[i]);
            temp.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            temp.setMinWidth(Region.USE_PREF_SIZE);
            temp.setPadding(new Insets(4,4,4,4));
            temp.setAlignment(Pos.CENTER);
            val[i+1] = temp;
        }
        records.addRow(0, val);
        //fill record data
        String[] rowData = new String[fields.length];
        for(int i=0;i<list.length;i++){
            if(recordType == "Patient"){
                selectedPatRec = (Scheduler.Patient) list[i];
                rowData = selectedPatRec.getValues();
            }else if(recordType == "Appointment"){
                selectedAppt = (Scheduler.Appointment) list[i];
                rowData = selectedAppt.getValues();
            }else if(recordType == "Employee"){
                selectedEmpRec = (Scheduler.Employee) list[i];
                rowData = selectedEmpRec.getValues();
            }else if(recordType == "Doctor"){
                selectedDocRec = (Scheduler.Doctor) list[i];
                rowData = selectedDocRec.getValues();
            }else{
                selectedAppt = (Scheduler.Appointment) list[i];
                rowData = selectedAppt.getValues();
            }
            ToggleButton rowSelector = new ToggleButton("Edit");
            rowSelector.setToggleGroup(dataBaseSelection);
            rowSelector.setUserData(rowData[0]);
            for(int j=0;j<rowData.length;j++){
                Label value = new Label(rowData[j]);
                value.setFont(Font.font("Arial", 14));
                value.setMinWidth(Region.USE_PREF_SIZE);
                value.setPadding(new Insets(4,4,4,4));
                value.setAlignment(Pos.CENTER);
                records.add(value, j+1, i+1);
            }

            if(sessionType == "Receptionist") {
                records.add(rowSelector, 0, i + 1);
            }
        }
        ScrollPane recordData = new ScrollPane();
        recordData.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        recordData.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        recordData.setContent(records);
        return recordData;
    }

    public BorderPane createDayView(){
        BorderPane dayView = new BorderPane();
        dayView.setPrefSize(600,550);
        //top: searchBar
        HBox searchBar = new HBox();
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setStyle("-fx-padding: 5;" +
                "-fx-background-color: DAE6F3;");
        Label search = new Label("Find Date: ");
        search.setFont(Font.font("Arial",FontWeight.BOLD,FontPosture.ITALIC,16));
        DatePicker searchDate = new DatePicker();
        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(event -> {
            //selected day
            if (today.equals(searchDate.getValue()) || searchDate.getValue() == null) {
                prevDayBtn.setDisable(true);
            }else if(searchDate.getValue().isBefore(today)){
                Alert oldDateSelection = new Alert(Alert.AlertType.ERROR);
                oldDateSelection.setHeaderText("Invalid Date Search");
                oldDateSelection.setContentText("Cannot view past schedules");
                oldDateSelection.showAndWait();
                searchDate.setValue(currentDay);
                data.setCenter(createDayView());
            }
            else {
                currentDay = searchDate.getValue();
                date.setText(currentDay.getMonth().toString() + " " + currentDay.getDayOfMonth() + ", " + currentDay.getYear());
                data.setCenter(createDayView());
            }
        });
        searchBar.getChildren().addAll(search, searchDate,searchBtn);
        searchBar.setSpacing(2);
        dayView.setTop(searchBar);
        //right: nextDayBtn
        nextDayBtn = new Button(">");
        nextDayBtn.setOnAction(event -> {
            currentDay = currentDay.plusDays(1);
            date.setText(currentDay.getMonth().toString() + " " + currentDay.getDayOfMonth() +", "+ currentDay.getYear());
            data.setCenter(createDayView());
            if(currentDay == today)
                prevDayBtn.setDisable(false);
        });
        nextDayBtn.setPrefSize(30, 400);
        HBox box = new HBox(nextDayBtn);
        box.setAlignment(Pos.CENTER);
        dayView.setRight(box);
        //left: prevDayBtn
        prevDayBtn = new Button("<");
        prevDayBtn.setOnAction(event -> {
            currentDay = currentDay.minusDays(1);
            date.setText(currentDay.getMonth().toString() + " " + currentDay.getDayOfMonth() +", "+ currentDay.getYear());
            data.setCenter(createDayView());
            if(currentDay.getDayOfYear() == today.getDayOfYear() && currentDay.getYear() == today.getYear())
                prevDayBtn.setDisable(true);
        });
        prevDayBtn.setPrefSize(30, 400);
        if(currentDay == today)
            prevDayBtn.setDisable(true);
        box = new HBox(prevDayBtn);
        box.setAlignment(Pos.CENTER);
        dayView.setLeft(box);
        //center: dayview appointment data
        dayView.setCenter(getAppointmentData());
        return dayView;
    }

    public BorderPane getAppointmentData(){
        BorderPane appointmentData = new BorderPane();
        int apptHeight = 500;
        appointmentData.setPrefSize(600, apptHeight);
        //left: times
        VBox times = new VBox();
        times.setPrefSize(75, apptHeight-100);
        HBox slot;
        Label header = new Label("Times");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        slot = new HBox(header);
        slot.setPrefSize(20, 50);
        slot.setAlignment(Pos.CENTER);
        times.getChildren().add(slot);
        for(int i=7; i<18; i++){
            String timestamp;
            if(i < 12){
                timestamp = i + "am";
            }else if(i==12){
                timestamp = i +"pm";
            }else{
                timestamp = (i-12) + "pm";
            }
            Label t = new Label(timestamp);
            t.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            slot = new HBox(t);
            slot.setPrefSize(20, (apptHeight-50)/11);
            slot.setAlignment(Pos.CENTER);
            times.getChildren().add(slot);
        }
        appointmentData.setLeft(times);
        if(sessionType == "Receptionist") {
            appointmentData.setCenter(fillAppointmentData());
        }else{
            appointmentData.setCenter(fillDoctorAppointmentData());
        }
        return appointmentData;
    }

    public ScrollPane fillDoctorAppointmentData(){
        //temp
        String doctorID = sessionUserID;
        Scheduler.Appointment appt;
        ArrayList<Scheduler.Appointment> doctorsAppt = dbAccessor.getAppointments(currentDay, Integer.parseInt(doctorID));
        HBox data = new HBox();
        VBox col = new VBox();
        Label header = new Label("Appt ID");
        header.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        header.setPrefSize(100, 50);
        header.setAlignment(Pos.CENTER);
        col.getChildren().add(header);
        Label timeslot;
        for(int i=0; i< doctorsAppt.size();i++){
            if(doctorsAppt.get(i) == null){
                timeslot = new Label("N/A");
            }else{
                timeslot = new Label(doctorsAppt.get(i).getId() +"");
            }
            timeslot.setAlignment(Pos.CENTER);
            timeslot.setFont(Font.font("Arial", 16));
            timeslot.setPrefSize(110, 450/11);
            col.getChildren().add(timeslot);
        }
        data.getChildren().add(col);
        ScrollPane content = new ScrollPane();
        content.setContent(data);
        return content;
    }

    public ScrollPane fillAppointmentData(){
        dayViewAppointments = new ToggleGroup();
        dayViewAppointments.selectedToggleProperty().addListener(timeSlotSelection);
        //temp
        Scheduler.Doctor[] doctorList = dbAccessor.getDoctorRecords().toArray(new Scheduler.Doctor[dbAccessor.getDoctorRecords().size()]);
        ToggleButton timeslot;
        ScrollPane content = new ScrollPane();
        int apptHeight = 500;
        VBox col;
        HBox data = new HBox();
        for(int i=0; i<doctorList.length;i++){
            //doctor label = doctorName + ID
            String name = doctorList[i].getD_Name();
            String dID = Integer.toString(doctorList[i].getId());
            Label doctor = new Label(name + "\nID#: "+dID);
            doctor.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            HBox header = new HBox(doctor);
            header.setPrefSize(50, 50);
            header.setAlignment(Pos.CENTER);
            col = new VBox();
            col.getChildren().add(header);
            //get appointments for each doctor
            Scheduler.Appointment[] apptList = dbAccessor.getAppointments(currentDay, doctorList[i].getId()).toArray(new Scheduler.Appointment[11]);
            for(int j=0; j<11; j++){
                //get time for user data
                String time;
                if(j<5){
                    time = (j+7) + "am";
                }else if(j==5){
                    time = (j+7) + "pm";
                }else{
                    time = (j-5) + "pm";
                }
                //set timeslot toggle buttons for current doctor/column; buttonText = N/A for empty slot, ApptID#: ## for taken slot
                String buttonText;
                if(apptList[j] == null){
                    //empty time slot
                    buttonText = "N/A";
                    timeslot = new ToggleButton("N/A");
                    timeslot.setStyle("-fx-border-style: solid inside;"+
                            "fx-border-width: 1px;"+
                            "fx-border-color: black;");
                    timeslot.setToggleGroup(dayViewAppointments);
                }else{
                    //taken time slot
                    buttonText = "ApptID# " + apptList[j].getId()+"";
                    timeslot = new ToggleButton(buttonText);
                    timeslot.setStyle("-fx-border-style: solid inside;"+
                            "fx-border-width: 1px;"+
                            "fx-border-color: black;");
                    timeslot.setToggleGroup(dayViewAppointments);
                }
                //userData: buttonText (availability), date, time, dID)
                timeslot.setUserData(new TimeSlot(buttonText, currentDay.getMonth() + " " +currentDay.getDayOfMonth() + ", " + currentDay.getYear(), time, dID));
                timeslot.setPrefSize(110,(apptHeight-50)/11);
                col.getChildren().add(timeslot);
            }
            data.getChildren().add(col);
        }
        content.setContent(data);
        content.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        content.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return content;
    }

    public void setViewOptions(){
        functions = new BorderPane();
        functions.setPrefSize(200, 600);
        functions.setStyle("-fx-padding: 5;" +
                "-fx-background-color: DAE6F3;");
        VBox view = new VBox();
        view.setSpacing(5);
        view.setPrefSize(200, 200);
        viewOptions = new ToggleGroup();
        Label header = new Label("View Options");
        header.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 16));
        dayView = new RadioButton("Day View");
        dayView.setToggleGroup(viewOptions);
        dayView.setSelected(true);
        dayView.setOnAction(changeViewEvent);
        patientDBView = new RadioButton("Patient Records");
        patientDBView.setToggleGroup(viewOptions);
        patientDBView.setOnAction(changeViewEvent);
        employeeDBView = new RadioButton("Employee Records");
        employeeDBView.setToggleGroup(viewOptions);
        employeeDBView.setOnAction(changeViewEvent);
        doctorDBView = new RadioButton("Doctor Records");
        doctorDBView.setToggleGroup(viewOptions);
        doctorDBView.setOnAction(changeViewEvent);
        appointmentDBView = new RadioButton("Appointment Records");
        appointmentDBView.setToggleGroup(viewOptions);
        appointmentDBView.setOnAction(changeViewEvent);
        dayView.setPrefWidth(200);
        patientDBView.setPrefWidth(200);
        employeeDBView.setPrefWidth(200);
        doctorDBView.setPrefWidth(200);
        appointmentDBView.setPrefWidth(200);
        if(sessionType == "Receptionist"){
            view.getChildren().addAll(header, dayView, patientDBView, employeeDBView, doctorDBView, appointmentDBView);
        }else if(sessionType == "Doctor"){
            //doctor can view: dayview of their appointments, patientDB, and appointmentDB of only their appointments
            appointmentDBView = new RadioButton("Your Appointments");
            appointmentDBView.setPrefWidth(200);
            appointmentDBView.setOnAction(changeViewEvent);
            appointmentDBView.setToggleGroup(viewOptions);
            view.getChildren().addAll(header, dayView, appointmentDBView, patientDBView);
        }else{
            //employee can view: dayview and all appointments
            view.getChildren().addAll(header, dayView, appointmentDBView);
        }
        view.setAlignment(Pos.TOP_CENTER);
        functions.setTop(view);
        //set bottom: logout + exit program + view Profile
        viewProfileBtn = new Button("View Profile");
        viewProfileBtn.setPrefWidth(200);
        viewProfileBtn.setOnAction(viewProfileEvent);
        logoutBtn = new Button("Log Out");
        logoutBtn.setPrefWidth(200);
        logoutBtn.setOnAction(event -> {
            primaryStage.setScene(createLoginScene());
        });
        exitBtn = new Button("Exit Program");
        exitBtn.setPrefWidth(200);
        exitBtn.setOnAction(event -> {
            primaryStage.close();
        });
        VBox bottom;
        if(sessionType == "Receptionist"){
            bottom = new VBox(reminderBtn,viewProfileBtn,logoutBtn,exitBtn);
        }else{
            bottom = new VBox(viewProfileBtn,logoutBtn,exitBtn);
        }
        functions.setBottom(bottom);
    }

    EventHandler<ActionEvent> reminderListEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            Stage apptReminderWindow = new Stage();
            apptReminderWindow.setTitle("Appointment Reminder List");
            BorderPane reminderLayout = new BorderPane();
            reminderLayout.setPrefSize(300,200);
            ScrollPane sp = new ScrollPane();
            GridPane apptRemGP = new GridPane();
            apptRemGP.setPadding(new Insets(5,5,5,5));
            apptRemGP.setAlignment(Pos.TOP_LEFT);
            apptRemGP.setVgap(10);
            apptRemGP.setHgap(15);
            //headers: apptID, doctorID, patientID + name, patientPhone
            Label date = new Label("Appt Date");
            Label appt = new Label("ApptID");
            Label time = new Label("Appt Time");
            date.setFont(Font.font("Arial",FontWeight.BOLD,12));
            appt.setFont(Font.font("Arial",FontWeight.BOLD,12));
            time.setFont(Font.font("Arial",FontWeight.BOLD,12));
            date.setAlignment(Pos.CENTER);
            appt.setAlignment(Pos.CENTER);
            time.setAlignment(Pos.CENTER);
            apptRemGP.addRow(0,date, appt, time);
            ArrayList<Scheduler.Appointment> todaysAppt = dbAccessor.getApptsForRV(today.plusDays(1));
            if(todaysAppt.size() == 0){
                Label noAppt = new Label("No Appointments Tomorrow");
                noAppt.setFont(Font.font(20));
                HBox temp = new HBox(noAppt);
                temp.setAlignment(Pos.CENTER);
                sp.setContent(temp);
            }else{
                for(int i=0; i<todaysAppt.size();i++){
                    Scheduler.Appointment appointment = todaysAppt.get(i);
                    Label apptDate = new Label(appointment.getAppt_Date()+"");
                    Label apptID = new Label(appointment.getId()+"");
                    Label apptTime = new Label(appointment.getAppt_Time()+"");
                    apptDate.setAlignment(Pos.CENTER);
                    apptID.setAlignment(Pos.CENTER);
                    apptTime.setAlignment(Pos.CENTER);
                    apptRemGP.addRow(i+1, apptDate, apptID, apptTime);
                }
                sp.setContent(apptRemGP);
            }


            Button close = new Button("Close");
            close.setOnAction(event1 -> {
                apptReminderWindow.close();
                timeline.play();
                enablePrimary();
            });
            close.setPrefWidth(100);
            apptRemGP.setPrefSize(300,200);
            Label title = new Label("Appointment Reminders");
            title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            HBox container = new HBox(title);
            container.setAlignment(Pos.CENTER);
            container.setPadding(new Insets(5,5,5,5));
            reminderLayout.setTop(container);
            reminderLayout.setCenter(sp);
            container = new HBox(close);
            container.setAlignment(Pos.CENTER);
            reminderLayout.setBottom(container);
            apptReminderWindow.setScene(new Scene(reminderLayout));
            disablePrimary();
            apptReminderWindow.showAndWait();
            enablePrimary();
        }
    };

    public void setDataOptions(){
        VBox data = new VBox();
        data.setSpacing(5);
        Label header = new Label("Data Options");
        header.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 16));
        if(sessionType == "Receptionist"){
            data.getChildren().add(header);
            if(currentView == "Day View"){
                addBtn = new Button("Add Appointment");
                addBtn.setDisable(true);
                addBtn.setOnAction(addAppointmentEvent);
                delBtn = new Button("Delete Appointment");
                delBtn.setDisable(true);
                addBtn.setPrefWidth(200);
                delBtn.setPrefWidth(200);
                data.getChildren().addAll(addBtn, delBtn);
            }else if(currentView == "Appointment"){
                delBtn = new Button("Delete Appointment");
                delBtn.setPrefWidth(200);
                delBtn.setDisable(true);
                data.getChildren().add(delBtn);
            }else if(currentView == "Doctor"){
                addBtn = new Button("Add Doctor");
                addBtn.setOnAction(addDoctorEvent);
                addBtn.setDisable(false);
                delBtn = new Button("Delete Doctor");
                delBtn.setDisable(true);
                addBtn.setPrefWidth(200);
                delBtn.setPrefWidth(200);
                data.getChildren().addAll(addBtn, delBtn);
            }else if(currentView == "Employee"){
                addBtn = new Button("Add Employee");
                addBtn.setOnAction(addEmployeeEvent);
                addBtn.setDisable(false);
                delBtn = new Button("Delete Employee");
                delBtn.setDisable(true);
                addBtn.setPrefWidth(200);
                delBtn.setPrefWidth(200);
                data.getChildren().addAll(addBtn, delBtn);
            }else{
                addBtn = new Button("Add Patient");
                addBtn.setDisable(false);
                addBtn.setOnAction(addPatientEvent);
                editBtn = new Button("Edit Patient");
                editBtn.setDisable(true);
                delBtn = new Button("Delete Patient");
                delBtn.setDisable(true);
                addBtn.setPrefWidth(200);
                editBtn.setPrefWidth(200);
                delBtn.setPrefWidth(200);
                data.getChildren().addAll(addBtn, editBtn, delBtn);
            }
        }
        data.setAlignment(Pos.CENTER);
        functions.setCenter(data);
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

    public void handle (ChoiceBox<String> accountTypeDrop ) { // when the button is clicked
        String credential = accountTypeDrop.getValue();	// getting the option the user chose in dropdown
        String query;
        int ID = Integer.parseInt(usernameTF.getText());  // get the ID number from the input
        String pword = pwTF.getText().trim();
        if(credential=="Medical Employee" || credential =="Receptionist") {				// changes values for obtaining login from msaccess database
            query = "SELECT * FROM MedicalEmployee WHERE Med_Employee_ID = "+ ID + " AND ME_Password = " +pword;
        }
        else{
            query = "SELECT * FROM Doctor WHERE Doctor_ID = "+ ID + " AND D_Password = " +pword;
        }
        String dbdir = "c:/db/";
        File f = new File(dbdir);
        if(!f.exists())
            f.mkdir();
        String dbName = "SchedulerDB.accdb";
        String dbPath = dbdir + "/" +dbName;
        File f2 = new File(dbPath);
        if(!f2.exists()){
            InputStream is = Main.class.getResourceAsStream("database/" + dbName);
            try {
                Files.copy(is, f2.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        try{
            final String databaseURL = "jdbc:ucanaccess://" + dbPath;
            Connection con = DriverManager.getConnection(databaseURL);
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                if(credential == "Receptionist"){
                    if(rs.getBoolean(4)){
                        sessionUserID = ID+"";
                        sessionType = credential;
                        primaryStage.setScene(createHomeScene());
                    }else{
                        Alert wrongCred = new Alert(Alert.AlertType.ERROR);
                        wrongCred.setHeaderText("Invalid username or password");
                        wrongCred.setContentText("Please contact your administrator if you forgot your credentials");
                        wrongCred.showAndWait();
                    }
                }else{
                    sessionUserID = ID+"";
                    sessionType = credential;
                    primaryStage.setScene(createHomeScene());
                }
            }else {
                Alert wrongCred = new Alert(Alert.AlertType.ERROR);
                wrongCred.setHeaderText("Invalid username or password");
                wrongCred.setContentText("Please contact your administrator if you forgot your credentials");
                wrongCred.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Connection to database failed");
            Alert invalidUser = new Alert(Alert.AlertType.ERROR);
            invalidUser.setHeaderText("Invalid username or password");
            invalidUser.setContentText("Please contact your administrator if you forgot your credentials");
            invalidUser.showAndWait();
        }
    }

    ChangeListener<Toggle> timeSlotSelection = (observable, oldValue, newValue) -> {
        if(newValue == null){
            //unselect a selected time slot
            addBtn.setDisable(true);
            delBtn.setDisable(true);
        }else{
            //buttonData: 0-slotText(n/a) 1-date 2-time 3-doctorID
            String[] buttonData = newValue.getUserData().toString().split("-");
            selected = new TimeSlot(buttonData[0], buttonData[1], buttonData[2], buttonData[3]);
            //empty slot selected
            if(selected.isAvailable){
                addBtn.setDisable(false);
                delBtn.setDisable(true);
            }else{
                addBtn.setDisable(true);
                delBtn.setDisable(false);
            }
        }
    };

    public void disablePrimary(){
        functions.setDisable(true);
        data.setDisable(true);
    }
    public void enablePrimary(){
        functions.setDisable(false);
        data.setDisable(false);
    }

    EventHandler<ActionEvent> delRecordEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            disablePrimary();
            deleteRecordWindow = new Stage();
            Label confirmText = new Label("Delete this Record?");
            Button cancel = new Button("Cancel");
            Button confirm = new Button("Confirm");
            GridPane confirmDeletion = new GridPane();
            confirmDeletion.setPrefSize(250, 300);
            confirmDeletion.addRow(0, confirmText);
            confirmDeletion.addRow(1, cancel, confirm);
            deleteRecordWindow.setScene(new Scene(confirmDeletion));
            deleteRecordWindow.show();
            cancel.setOnAction(event1 -> {
                deleteRecordWindow.close();
                enablePrimary();
            });
            confirm.setOnAction(event1 -> {
                if(currentView == "Appointment"){
                    Scheduler.Appointment temp = new Scheduler.Appointment(Integer.parseInt(selectedRecordID));
                    dbAccessor.removeAppointment(temp);
                }else if(currentView == "Patient"){
                    Scheduler.Patient temp = new Scheduler.Patient(selectedRecordID);
                    dbAccessor.removePatientRecord(temp);
                }else if(currentView == "Doctor"){
                    Scheduler.Doctor temp = new Scheduler.Doctor(selectedRecordID);
                    dbAccessor.removeDoctorRecord(temp);
                }else{
                    Scheduler.Employee temp = new Scheduler.Employee(selectedRecordID);
                    dbAccessor.removeEmployeeRecord(temp);
                }
                Alert confirmDelet = new Alert(Alert.AlertType.CONFIRMATION);
                confirmDelet.setContentText("Record has been deleted");
                confirmDelet.showAndWait();
                deleteRecordWindow.close();
                setDataOptions();
                setData();
                enablePrimary();
            });

        }
    };

    EventHandler<ActionEvent> editPatientRecordEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            disablePrimary();
            ArrayList<Scheduler.Patient> pList = dbAccessor.getPatientRecords();
            int i=0;
            while(i<pList.size()){
                if(pList.get(i).getId() == Integer.parseInt(selectedRecordID)){
                    selectedPatRec = pList.get(i);
                    i = pList.size()-1;
                }else{
                    i++;
                }
            }
            editPatientRecordWindow = new Stage();
            GridPane selRecordData = new GridPane();
            Label header = new Label("Edit Patient: ");
            Label headerVal = new Label(""+selectedPatRec.getId());
            Label fname = new Label("First Name: ");
            Label lname = new Label("Last Name: ");
            Label DOB = new Label("DOB: ");
            Label SSN = new Label("SSN: ");
            Label phone = new Label("Phone: ");
            Label address = new Label("Address: ");
            Label email = new Label("Email: ");
            TextField first = new TextField(selectedPatRec.getFname());
            TextField last = new TextField(selectedPatRec.getLname());
            TextField dob = new TextField(selectedPatRec.getDOB());
            TextField ssn = new TextField(selectedPatRec.getSSN());
            TextField ph = new TextField(selectedPatRec.getPhone());
            TextField add = new TextField(selectedPatRec.getAddress());
            TextField em = new TextField(selectedPatRec.getEmail());
            selRecordData.addRow(0, header, headerVal);
            selRecordData.addRow(1, fname, first);
            selRecordData.addRow(2, lname, last);
            selRecordData.addRow(3, DOB, dob);
            selRecordData.addRow(4, SSN, ssn);
            selRecordData.addRow(5, phone, ph);
            selRecordData.addRow(6, address, add);
            selRecordData.addRow(7, email, em);
            Button cancel = new Button("Cancel");
            cancel.setOnAction(event1 -> {
                editPatientRecordWindow.close();
                enablePrimary();
            });
            Button update = new Button("Update");
            update.setOnAction(event1 -> {
                Scheduler.Patient updatedPatient = new Scheduler.Patient(selectedPatRec.getId(), first.getText(),
                        last.getText(), dob.getText(), ssn.getText(), ph.getText(), add.getText(), em.getText());
                dbAccessor.updatePatientRecord(updatedPatient);
                editPatientRecordWindow.close();
                enablePrimary();
            });
            selRecordData.addRow(8, cancel, update);
            selRecordData.setPrefSize(350,450);
            editPatientRecordWindow.setScene(new Scene(selRecordData));
            editPatientRecordWindow.showAndWait();
            setData();
            enablePrimary();
        }
    };

    EventHandler<ActionEvent> addAppointmentEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            //disable primaryStage components
            disablePrimary();
            Alert invalid = new Alert(Alert.AlertType.ERROR);
            invalid.setHeaderText("Invalid Appointment Date");
            if(currentDay.isBefore(today)){
                invalid.setContentText("Can only schedule a future appointment");
                invalid.showAndWait();
                enablePrimary();
            }else if(currentDay.isEqual(today)){
                //appointments on same day must be made at least 2 hours before appointment time
                int selectedHour = Integer.parseInt(selected.time.substring(0, selected.time.length()-2));
                if(selectedHour >= 1 && selectedHour <= 5){
                    selectedHour += 12;
                }
                int currentHour = LocalTime.now().getHour();
                //display alert if current hour is 2 hours greater than selected hour
                if(currentHour > selectedHour+2){
                    invalid.setContentText("Appointments must be scheduled at least 2 hours prior");
                    invalid.showAndWait();
                    enablePrimary();
                }
            }else {
                addAppointmentWindow = new Stage();
                addAppointmentWindow.setTitle("Appointment Scheduler");
                GridPane addApptForm = new GridPane();
                addApptForm.setAlignment(Pos.CENTER);
                addApptForm.setVgap(10);
                addApptForm.setPrefSize(350, 450);
                //Header
                HBox header = new HBox();
                Label heading = new Label("Appointment Information");
                heading.setFont(Font.font("Arial", FontWeight.BOLD, 20));
                header.setPrefHeight(50);
                header.getChildren().add(heading);
                addApptForm.addRow(0, header);
                //temp
                //form fields: patientID, doctorID(not selectable), date, time, reason
                //choose patient
                Label patient = new Label("Patient: ");
                patient.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                TextField searchPatientTF = new TextField("Enter Patient ID");
                searchPatientTF.textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        if(newValue.trim().isEmpty()){
                            scheduleBtn.setDisable(true);
                        }else{
                            scheduleBtn.setDisable(false);
                        }
                    }
                });
                //create search bar for patient
                HBox pBox = new HBox(patient, searchPatientTF);
                addApptForm.addRow(1, pBox);
                //get data from selected Time Slot
                //display date
                Label date = new Label("Date: ");
                date.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                Label selectedDate = new Label(selected.date);
                selectedDate.setFont(Font.font("Arial", FontPosture.ITALIC, 12));
                HBox dBox = new HBox(date, selectedDate);
                dBox.setSpacing(5);
                addApptForm.addRow(2, dBox);
                //display time/duration
                Label time = new Label("Time: ");
                time.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                Label selectedTime = new Label(selected.time);
                selectedTime.setFont(Font.font("Arial", FontPosture.ITALIC, 12));
                HBox tBox = new HBox(time, selectedTime);
                tBox.setSpacing(5);
                addApptForm.addRow(3, tBox);
                //display doctorID
                Label doc = new Label("Doctor: ");
                doc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                Label selectedDoctor = new Label(selected.doctorID);
                selectedDoctor.setFont(Font.font("Arial", FontPosture.ITALIC, 12));
                HBox docBox = new HBox(doc, selectedDoctor);
                docBox.setSpacing(5);
                addApptForm.addRow(4, docBox);
                Label reason = new Label("Appointment Reason (Optional)");
                reason.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                TextField reasonTF = new TextField();
                reasonTF.setPrefSize(300,100);
                reasonTF.positionCaret(0);
                addApptForm.addRow(5, reason);
                addApptForm.addRow(6, reasonTF);
                scheduleBtn = new Button("Schedule");
                scheduleBtn.setPrefWidth(100);
                scheduleBtn.setDisable(true);
                //get inputted patientID from searchPatientTF
                scheduleBtn.setOnAction(event1 -> {
                    String inputID = searchPatientTF.getText();
                    //check if empty
                    if(!inputID.trim().equals("")){
                        boolean allLetters = inputID.chars().allMatch(Character::isDigit);
                        //ID should include only numbers
                        Alert invalidID, createdAppt;
                        if(allLetters){
                            if(dbAccessor.patientExists(new Scheduler.Patient(inputID))){
                                int patientID = Integer.parseInt(inputID);
                                //create Appointment
                                Scheduler.Appointment newAppt = new Scheduler.Appointment();
                                newAppt.setPatient_Id(patientID);
                                newAppt.setAppt_Time(selected.getTime());
                                newAppt.setAppt_Date(selected.getDate());
                                newAppt.setDoctor_Id(Integer.parseInt(selected.doctorID));
                                if(reasonTF.getText().isEmpty()){
                                    newAppt.setReason("");
                                }else{
                                    newAppt.setReason(reasonTF.getText());
                                }
                                dbAccessor.createAppointment(newAppt);
                                createdAppt = new Alert(Alert.AlertType.CONFIRMATION);
                                createdAppt.setContentText("Appointment Created");
                                createdAppt.showAndWait();
                                addAppointmentWindow.close();
                                enablePrimary();
                                setData();
                                home.setRight(data);

                            }else{
                                invalidID = new Alert(Alert.AlertType.ERROR);
                                invalidID.setHeaderText("Appointment Cannot Be Created");
                                invalidID.setContentText("Selected Patient Not In System");
                                invalidID.showAndWait();
                            }
                        }else{
                            invalidID = new Alert(Alert.AlertType.ERROR);
                            invalidID.setHeaderText("Appointment Cannot Be Created");
                            invalidID.setContentText("Patient ID Invalid");
                            invalidID.showAndWait();
                        }
                    }else{
                        scheduleBtn.setDisable(true);
                    }
                });
                cancelBtn = new Button("Cancel");
                cancelBtn.setPrefWidth(100);
                cancelBtn.setOnAction(event1 -> {
                    addAppointmentWindow.close();
                });
                HBox schedButtons = new HBox(scheduleBtn, cancelBtn);
                schedButtons.setPrefHeight(50);
                schedButtons.setAlignment(Pos.CENTER);
                addApptForm.addRow(7, schedButtons);
                addAppointmentWindow.setScene(new Scene(addApptForm));
                addAppointmentWindow.showAndWait();
                enablePrimary();
            }
        }
    };

    EventHandler<ActionEvent> addEmployeeEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            addNonApptWindow = new Stage();
            BorderPane windowContent = new BorderPane();
            windowContent.setPrefSize(350, 500);
            Label header = new Label("Add Employee");
            header.setFont(Font.font(20));
            header.setAlignment(Pos.CENTER);
            header.setPrefSize(350, 100);
            windowContent.setTop(header);
            //form fields
            GridPane fields = new GridPane();
            Label name = new Label("Name: ");
            Label pw = new Label("Password: ");
            Label empType = new Label("Employee Type: ");
            TextField nameTF = new TextField();
            TextField pwTF = new TextField();
            ChoiceBox<String> employeeType = new ChoiceBox<>();
            createRecBtn = new Button("Create Employee");
            createRecBtn.setOnAction(event1 -> {
                boolean allNum = pwTF.getText().chars().allMatch(Character::isDigit);
                Alert invalidInput;
                if(nameTF.getText().trim() !="" && pwTF.getText().trim()!=""&& employeeType.getSelectionModel().getSelectedItem()!=null){
                    boolean receptionist = false;
                    if(allNum) {
                        if (employeeType.getSelectionModel().getSelectedItem() == "Receptionist") {
                            receptionist = true;
                        }
                        dbAccessor.createEmployeeRecord(new Scheduler.Employee(nameTF.getText(), pwTF.getText(), receptionist));
                        setData();
                        addNonApptWindow.close();
                        enablePrimary();
                    }else{
                        invalidInput = new Alert(Alert.AlertType.ERROR);
                        invalidInput.setContentText("Numeric Password Only");
                        invalidInput.showAndWait();
                    }
                }else{
                    invalidInput = new Alert(Alert.AlertType.ERROR);
                    invalidInput.setContentText("Please fill out the entire form");
                    invalidInput.showAndWait();
                }

            });
            cancelBtn = new Button("Cancel");
            cancelBtn.setOnAction(event1 -> {
                addNonApptWindow.close();
                enablePrimary();
            });
            cancelBtn.setPrefSize(100, 30);
            createRecBtn.setPrefSize(100,30);
            employeeType.getItems().addAll("Receptionist", "Other");
            fields.addRow(0, name, nameTF);
            fields.addRow(1, pw, pwTF);
            fields.addRow(2, empType, employeeType);
            fields.addRow(3, cancelBtn, createRecBtn);
            fields.setVgap(10);
            windowContent.setCenter(fields);
            addNonApptWindow.setScene(new Scene(windowContent));
            addNonApptWindow.showAndWait();

        }
    };

    EventHandler<ActionEvent> addDoctorEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            addNonApptWindow = new Stage();
            BorderPane windowContent = new BorderPane();
            windowContent.setPrefSize(350, 500);
            Label header = new Label("Add Doctor");
            header.setFont(Font.font(20));
            header.setAlignment(Pos.CENTER);
            header.setPrefSize(350, 100);
            windowContent.setTop(header);
            //form fields
            GridPane fields = new GridPane();
            Label name = new Label("Name: ");
            Label phone = new Label("Phone: ");
            Label pw = new Label("Password: ");
            TextField nameTF = new TextField();
            TextField phoneTF = new TextField();
            TextField pwTF = new TextField();
            createRecBtn = new Button("Create Dooctor");
            createRecBtn.setOnAction(event1 -> {
                boolean allNum = pwTF.getText().chars().allMatch(Character::isDigit);
                Alert invalidInput;
                if(nameTF.getText().trim() !="" && pwTF.getText().trim()!=""&& phoneTF.getText().trim() != ""){
                    if(allNum) {
                        dbAccessor.createDoctorRecord(new Scheduler.Doctor(nameTF.getText(), phoneTF.getText(), pwTF.getText()));
                        setData();
                        addNonApptWindow.close();
                        enablePrimary();
                    }else{
                        invalidInput = new Alert(Alert.AlertType.ERROR);
                        invalidInput.setContentText("Numeric Password Only");
                        invalidInput.showAndWait();
                    }
                }else{
                    invalidInput = new Alert(Alert.AlertType.ERROR);
                    invalidInput.setContentText("Please fill out the entire form");
                    invalidInput.showAndWait();
                }

            });
            cancelBtn = new Button("Cancel");
            cancelBtn.setOnAction(event1 -> {
                addNonApptWindow.close();
                enablePrimary();
            });
            cancelBtn.setPrefSize(200, 30);
            createRecBtn.setPrefSize(200,30);
            fields.addRow(0, name, nameTF);
            fields.addRow(1, pw, pwTF);
            fields.addRow(2, phone, phoneTF);
            fields.addRow(3, cancelBtn, createRecBtn);
            fields.setVgap(10);
            fields.setAlignment(Pos.CENTER);
            windowContent.setCenter(fields);
            addNonApptWindow.setScene(new Scene(windowContent));
            addNonApptWindow.showAndWait();

        }
    };

    EventHandler<ActionEvent> addPatientEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            addNonApptWindow = new Stage();
            BorderPane windowContent = new BorderPane();
            windowContent.setPrefSize(400, 600);
            Label header = new Label("Add Patient");
            header.setFont(Font.font(20));
            header.setAlignment(Pos.CENTER);
            header.setPrefSize(350, 100);
            windowContent.setTop(header);
            //form fields
            GridPane fields = new GridPane();
            Label fname = new Label("First Name: ");
            Label lname = new Label("Last Name: ");
            Label dob = new Label("DOB: ");
            Label ssn = new Label("SSN: ");
            Label phone = new Label("Phone: ");
            Label address = new Label("Address: ");
            Label email = new Label("Email: ");
            TextField fnameTF = new TextField();
            TextField lnameTF = new TextField();
            TextField dobTF = new TextField();
            TextField ssnTF = new TextField();
            TextField phoneTF = new TextField();
            TextField addressTF = new TextField();
            TextField emailTF = new TextField();
            TextField[] formVal = {fnameTF, lnameTF, dobTF, ssnTF, phoneTF, addressTF, emailTF};
            createRecBtn = new Button("Create Patient");
            createRecBtn.setOnAction(event1 -> {
                Alert invalidInput;
                boolean emptyField = false;
                int i=0;
                while(i<formVal.length){
                    if(formVal[i].getText().trim()==""){
                        emptyField = true;
                        break;
                    }else{
                        i+=1;
                    }
                }
                if(emptyField){
                    invalidInput = new Alert(Alert.AlertType.ERROR);
                    invalidInput.setContentText("Please fill out the entire form");
                }else{
                    dbAccessor.createPatientRecord(new Scheduler.Patient(
                            formVal[0].getText(),   //first name
                            formVal[1].getText(),   //last name
                            formVal[2].getText(),   //date of birth
                            formVal[3].getText(),   //ssn
                            formVal[4].getText(),   //phone
                            formVal[5].getText(),   //address
                            formVal[6].getText())); //email
                    setData();
                    addNonApptWindow.close();
                    enablePrimary();
                }
            });
            cancelBtn = new Button("Cancel");
            cancelBtn.setOnAction(event1 -> {
                addNonApptWindow.close();
                enablePrimary();
            });
            cancelBtn.setPrefSize(200, 30);
            createRecBtn.setPrefSize(200,30);
            fields.addRow(0, fname, fnameTF);
            fields.addRow(1, lname, lnameTF);
            fields.addRow(2, dob, dobTF);
            fields.addRow(3, ssn, ssnTF);
            fields.addRow(4, phone, phoneTF);
            fields.addRow(5, address, addressTF);
            fields.addRow(6,email, emailTF);
            fields.addRow(7, cancelBtn, createRecBtn);
            fields.setVgap(5);
            fields.setAlignment(Pos.CENTER);
            windowContent.setCenter(fields);
            addNonApptWindow.setScene(new Scene(windowContent));
            addNonApptWindow.showAndWait();

        }
    };

    EventHandler<ActionEvent> changeViewEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if(dayView.isSelected()){
                currentView = "Day View";
            }else if(employeeDBView.isSelected()){
                currentView = "Employee";
            }else if(doctorDBView.isSelected()){
                currentView = "Doctor";
            }else if(patientDBView.isSelected()){
                currentView = "Patient";
            }else if(appointmentDBView.isSelected()){
                currentView = "Appointment";
            }else{
                currentView = "Day View";
                dayView.setSelected(true);
            }
            setDataOptions();
            setData();
        }
    };

    EventHandler<ActionEvent> viewProfileEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            disablePrimary();
            String userPw = "No Password in System", userName = "No Name in System";
            if(sessionType == "Doctor"){
                ArrayList<Scheduler.Doctor> d = dbAccessor.getDoctorRecords();
                int i=0;
                while(i<d.size()){
                    if(d.get(i).getId() == Integer.parseInt(sessionUserID)){
                        userPw = d.get(i).getD_Password();
                        userName = d.get(i).getD_Name();
                        selectedDocRec = new Scheduler.Doctor(Integer.parseInt(sessionUserID), userName, d.get(i).getPhone(), userPw);
                        break;
                    }else{
                        i++;
                    }
                }
            }else{
                ArrayList<Scheduler.Employee> e = dbAccessor.getEmployeeRecords();
                int i=0;
                while(i<e.size()){
                    if(e.get(i).getId() == Integer.parseInt(sessionUserID)){
                        userPw = e.get(i).getE_Password();
                        userName = e.get(i).getE_Name();
                        boolean isRec = false;
                        if(sessionType == "Receptionist"){
                            isRec = true;
                        }
                        selectedEmpRec = new Scheduler.Employee(Integer.parseInt(sessionUserID),userName, userPw,isRec);
                        break;
                    }else{
                        i++;
                    }
                }
            }
            userProfileWindow = new Stage();
            userProfileWindow.setTitle("Your Profile");
            userProfileWindow.setResizable(false);
            GridPane profile = new GridPane();
            profile.setAlignment(Pos.CENTER);
            profile.setPadding(new Insets(10,10,10,10));
            profile.setPrefSize(350, 400);
            profile.setHgap(5);
            profile.setVgap(20);
            Label header = new Label("Profile: ");
            header.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            header.setPrefHeight(50);
            Label userHeading = new Label(userName);
            userHeading.setPrefHeight(50);
            userHeading.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            profile.addRow(0, header, userHeading);
            Label username = new Label("Employee ID: ");
            username.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            Label currentUN = new Label(sessionUserID);
            currentUN.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 16));
            userHeading.setFont(Font.font(14));
            profile.addRow(1, username, currentUN);
            Label pw = new Label("Password: ");
            pw.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            currentPW = new TextField(userPw);
            Button update = new Button("Update");
            currentPW.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    update.setDisable(false);
                }
            });
            profile.addRow(2, pw, currentPW);
            Button cancel = new Button("Exit");
            cancel.setPrefWidth(75);
            cancel.setOnAction(event1 -> {
                userProfileWindow.close();
                userProfileWindow = new Stage();
            });
            update.setPrefWidth(75);
            update.setDisable(true);
            update.setOnAction(event1 -> {
                if(sessionType == "Doctor"){
                    dbAccessor.updateDoctorRecord(selectedDocRec);
                }else{
                    dbAccessor.updateEmployeeRecord(selectedEmpRec);
                }
            });
            profile.addRow(3, cancel, update);
            userProfileWindow.setScene(new Scene(profile));
            userProfileWindow.setAlwaysOnTop(true);
            userProfileWindow.showAndWait();
            enablePrimary();
        }
    };

    private class TimeSlot{
        String date, time, doctorID, slotText;
        boolean isAvailable;
        private TimeSlot(String text, String d, String t, String doctor){
            slotText = text;
            if(slotText.equals("N/A")){
                isAvailable = true;
            }else{
                isAvailable = false;
            }
            date = d;
            time = t;
            doctorID = doctor;
        }
        public String toString(){
            return slotText + "-" + date + "-" + time + "-" + doctorID;
        }
        public LocalTime getTime(){
            char[] t = time.toCharArray();
            String hour;
            if (t.length == 4) {
                hour = time.substring(0, 1);
            }else{
                hour = time.charAt(0) + "";
            }
            return LocalTime.of(Integer.parseInt(hour), 0);
        }
        public LocalDate getDate(){
            String[]d = date.split(" ");
            int year = Integer.parseInt(d[2]);
            int day = Integer.parseInt(d[1].substring(0, d[1].indexOf(",")));
            int month;
            switch (d[0]){
                case "JANUARY":
                    month = 1;
                    break;
                case "FEBRUARY":
                    month = 2;
                    break;
                case "MARCH":
                    month = 3;
                    break;
                case "APRIL":
                    month = 4;
                    break;
                case "MAY":
                    month = 5;
                    break;
                case "JUNE":
                    month = 6;
                    break;
                case "JULY":
                    month = 7;
                    break;
                case "AUGUST":
                    month = 8;
                    break;
                case "SEPTEMBER":
                    month = 9;
                    break;
                case "OCTOBER":
                    month =10;
                    break;
                case "NOVEMBER":
                    month = 11;
                    break;
                default:
                    month = 12;

            }
            return LocalDate.of(year, month, day);
        }
    }

    private class LoginTextFieldListener implements ChangeListener<String> {
        @Override
        public void changed(ObservableValue<? extends String> source, String oldValue, String newValue) {
            String usernameVal = usernameTF.getText();
            String passwordVal = pwTF.getText();
            loginBtn.setDisable(usernameVal.trim().equals("") || passwordVal.trim().equals(""));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}