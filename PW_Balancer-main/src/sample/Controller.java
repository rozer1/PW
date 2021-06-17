package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

import javax.swing.*;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Math.log;
import static javafx.application.Platform.*;

public class Controller implements Initializable {
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    @FXML
    public TableView<TableData> queueTable;
    @FXML
    public TableColumn<TableData, String> arriveData;
    @FXML
    public TableColumn<TableData, Integer> clientID;
    @FXML
    public TableColumn<TableData, Integer> clientFileAmount;
    @FXML
    public TableColumn<TableData, Integer> clientFilesSize;
    @FXML
    private TableColumn<TableData,Double> proggresBar;
    @FXML
    public TableColumn<TableData, Double> clientPrior;
    @FXML
    public Label disc0, disc1, disc2, disc3, disc4;
    @FXML
    private Button button;

    public static boolean[] discs_available = new boolean[]{true, true, true, true, true};
    public static int[] current_file_size = new int[]{0, 0, 0, 0, 0};
    public static int[] current_client_id = new int[]{0, 0, 0, 0, 0};

    int CLIENT_AMOUNT = 30;
    int MIN_FILE_SIZE = 1, MAX_FILE_SIZE = 100;
    int MIN_FILE_AMOUNT = 1, MAX_FILE_AMOUNT = 10;//for one client

    Random random = new Random();
    int client_id = 0;


    private final ObservableList<TableData> data = FXCollections.observableArrayList();


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        queueTable.setItems(data);

        arriveData.setCellValueFactory(new PropertyValueFactory<>("arriveData"));
        clientID.setCellValueFactory(new PropertyValueFactory<>("clientID"));
        clientFileAmount.setCellValueFactory(new PropertyValueFactory<>("clientFileAmount"));
        clientFilesSize.setCellValueFactory(new PropertyValueFactory<>("clientFilesSize"));
        clientPrior.setCellValueFactory(new PropertyValueFactory<>("clientPrior"));
        proggresBar.setCellValueFactory(new PropertyValueFactory<TableData, Double>("proggresBar"));
        proggresBar.setCellFactory(ProgressBarTableCell.<TableData> forTableColumn());


        queueTable.setPlaceholder(new Label("Brak klientów"));
        createDiscs();
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {

                generateClients(CLIENT_AMOUNT, MIN_FILE_SIZE, MAX_FILE_SIZE, MIN_FILE_AMOUNT, MAX_FILE_AMOUNT);
                System.out.println("Pressed!");
            }
        });
        manageDiscs();
    }

        public void manageDiscs() {

        Thread manageThread = new Thread(() -> {
            while (true) {
                for (int i = 0; i < 5; i++) {//true means disc unused
                    if (discs_available[i] && data.size() != 0) {
                        try {
                            UploadedFile chosenFile = get_current_biggest_prior();
                            if(chosenFile!=null){
                                current_client_id[i] = chosenFile.getClientID();
                                current_file_size[i] = chosenFile.getFileSize();
                                discs_available[i] = false;
                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        manageThread.start();
    }

    private void generateClients(int CLIENT_AMOUNT, int MIN_FILE_SIZE, int MAX_FILE_SIZE, int MIN_FILE_AMOUNT, int MAX_FILE_AMOUNT) {

        Thread generateClients = new Thread(() -> {
            while (client_id < this.CLIENT_AMOUNT) {
                int files_amount = random.nextInt(this.MAX_FILE_AMOUNT - this.MIN_FILE_AMOUNT) + this.MIN_FILE_AMOUNT;
                long current_time = System.currentTimeMillis();
                int files_size = 0;
                ArrayList<Integer> files = new ArrayList<>();
                for (int i = 0; i < files_amount; i++) {
                    int curr_file = random.nextInt(this.MAX_FILE_SIZE - this.MIN_FILE_SIZE) + this.MIN_FILE_SIZE;
                    files_size += curr_file;
                    files.add(curr_file);
                }
                data.add(new TableData(client_id, formatter.format(current_time), files_amount, files_size, 0.0,files, 0.0));
                client_id++;
                //one second wait until next client arrive
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        generateClients.start();
        client_id=0;

    }

    private void createDiscs() {
        for (int i = 0; i < 5; i++) {//run all discs
            int finalI = i;
            Thread taskThread = new Thread(() -> {

                while (true) {
                    if (finalI == 0) {
                        manageDisc(finalI, disc0);
                    }
                    if (finalI == 1) {
                        manageDisc(finalI, disc1);
                    }
                    if (finalI == 2) {
                        manageDisc(finalI, disc2);
                    }
                    if (finalI == 3) {
                        manageDisc(finalI, disc3);
                    }
                    if (finalI == 4) {
                        manageDisc(finalI, disc4);
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            taskThread.start();
        }
    }

    private void manageDisc(int disk_number, Label disc5) {
        try {
            if (!discs_available[disk_number]) {
                runLater(() -> disc5.setText("Klient: " + current_client_id[disk_number]));
                Thread.sleep((long) 100 * current_file_size[disk_number]);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runLater(() -> disc5.setText("Oczekuje na dane"));
        discs_available[disk_number] = true;
        current_file_size[disk_number] = 0;
    }

    UploadedFile get_current_biggest_prior() throws ParseException {

        long current_time = System.currentTimeMillis();

        long longest_wait = current_time - new Timestamp(formatter.parse(data.get(0).getArriveData()).getTime()).getTime();
        int biggest_file = data.get(0).getClientFilesCurrentSize();
        for (TableData t : data){
            if (longest_wait < current_time - new Timestamp(formatter.parse(t.getArriveData()).getTime()).getTime())
                longest_wait = current_time - new Timestamp(formatter.parse(t.getArriveData()).getTime()).getTime();
            if (biggest_file < t.getClientFilesCurrentSize())
                biggest_file = t.getClientFilesCurrentSize();
        }
        if(biggest_file==0){
            return null;
        }
        double max_prior = 0.0;
        TableData prior_file = data.get(0);

        for (TableData datum : data) {
            if (datum.getClientFileAmount()>0){
                double temp = count_prior(datum.getClientFilesCurrentSize(), current_time - new Timestamp(formatter.parse(datum.getArriveData()).getTime()).getTime(), longest_wait, biggest_file, datum.getClientID());
                datum.setClientPrior(temp);
                if (temp > max_prior) {
                    max_prior = temp;
                    prior_file = datum;
                }
            }
            else {
                datum.setClientPrior(0);
                datum.setProggresBar(0.0);
            }

        }

        int current_file = prior_file.getOneClientFile();
        prior_file.ilosc-=1;
        prior_file.setClientFilesSize(prior_file.getClientFilesCurrentSize());
        prior_file.setClientFileAmount(prior_file.ilosc);
        double proggressBar = count_proggressBar(prior_file.ilosc);
        System.out.println(proggressBar + "proggressBar");
        prior_file.setProggresBar(proggressBar);
        if(prior_file.ilosc==0)
           data.remove(prior_file);


        return new UploadedFile(prior_file.getClientID(),current_file);
    }

    double count_proggressBar(int file){
        double state =0.0;
        switch (file){
            case 10:
                state = 0.0;
                break;
            case 9:
                state = 0.1;
                break;
            case 8:
                state = 0.2;
                break;
            case 7:
                state = 0.3;
                break;
            case 6:
                state = 0.4;
                break;
            case 5:
                state = 0.5;
                break;
            case 4:
                state = 0.6;
                break;
            case 3:
                state = 0.7;
                break;
            case 2:
                state = 0.8;
                break;
            case 1:
                state = 0.9;
                break;
            case 0:
                state = 1.0;
                break;

        }
        return state;
    }
    double count_prior(int file_size, long wait_timef, long wait_time, int BIGGEST_FILE, int z) {
        //System.out.println("Biggest File:"+BIGGEST_FILE+ " Max_waiting:"+MAX_WAITING+" Wait_time:"+wait_time+" File_size:"+file_size);
        double size = (double) 4 / (file_size + 1);
        System.out.println(size+"  size");
        double arrive = log(wait_time+1)-z/5;
        System.out.println(arrive+"  arrive");
        return  size + arrive;//qw
    }
    public class ProgressWorker extends SwingWorker<Object, Object> {

        @Override
        protected Object doInBackground() throws Exception {

            for (int i = 0; i < 100; i++) {
                setProgress(i);
                try {
                    Thread.sleep(25);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
}

