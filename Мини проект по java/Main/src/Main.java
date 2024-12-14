import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.text.NumberFormat;
import java.text.ParseException;

// Класс Transport
class Transport {
    private final double weight;
    private final double maxSpeed;

    public Transport(double weight, double maxSpeed) {
        this.weight = weight;
        this.maxSpeed = maxSpeed;
    }

    public double getWeight() {
        return weight;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }
}

// Класс Airplane, наследник Transport
class Airplane extends Transport {
    private double wingSpan;
    private double maxAltitude;

    public Airplane(double weight, double maxSpeed, double wingSpan, double maxAltitude) {
        super(weight, maxSpeed);
        this.wingSpan = wingSpan;
        this.maxAltitude = maxAltitude;
    }

    public double getWingSpan() {
        return wingSpan;
    }

    public double getMaxAltitude() {
        return maxAltitude;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Самолет [Вес: %.2f, Макс. скорость: %.2f, Размах крыльев: %.2f, Макс. высота: %.2f]",
                getWeight(), getMaxSpeed(), wingSpan, maxAltitude);
    }
}

// Класс для работы с самолетами
class AirplaneHandler {
    public List<Airplane> generateAirplanes(int count) {
        Random random = new Random();
        List<Airplane> airplanes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            airplanes.add(new Airplane(
                    1000 + random.nextDouble() * 9000, // Вес от 1000 до 10000
                    200 + random.nextDouble() * 800,   // Скорость от 200 до 1000
                    10 + random.nextDouble() * 40,    // Размах крыла от 10 до 50
                    5000 + random.nextDouble() * 10000 // Высота от 5000 до 15000
            ));
        }
        return airplanes;
    }

    public double calculateAverageSpeed(List<Airplane> airplanes) {
        return airplanes.stream()
                .mapToDouble(Airplane::getMaxSpeed)
                .average()
                .orElse(0);
    }

    public Airplane findMaxWeightAirplane(List<Airplane> airplanes) {
        return airplanes.stream()
                .max(Comparator.comparingDouble(Airplane::getWeight))
                .orElse(null);
    }

    public void saveAirplanesToFile(List<Airplane> airplanes, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Airplane airplane : airplanes) {
                writer.write(airplane.toString());
                writer.newLine();
            }
        }
    }

    public List<Airplane> loadAirplanesFromFile(String filename) throws IOException {
        List<Airplane> airplanes = new ArrayList<>();
        NumberFormat format = NumberFormat.getInstance(Locale.US);
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Пример примитивного парсинга (не рекомендуется для сложных проектов)
                String[] parts = line.replace("Самолет [", "")
                        .replace("]", "")
                        .split(", ");
                try {
                    double weight = format.parse(parts[0].split(": ")[1]).doubleValue();
                    double maxSpeed = format.parse(parts[1].split(": ")[1]).doubleValue();
                    double wingSpan = format.parse(parts[2].split(": ")[1]).doubleValue();
                    double maxAltitude = format.parse(parts[3].split(": ")[1]).doubleValue();
                    airplanes.add(new Airplane(weight, maxSpeed, wingSpan, maxAltitude));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return airplanes;
    }
}

// Класс DataBase
class DataBase {
    private static final String URL = "jdbc:sqlite:airplanes.db";

    public void createTable() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS airplanes ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "weight REAL,"
                    + "maxSpeed REAL,"
                    + "wingSpan REAL,"
                    + "maxAltitude REAL"
                    + ");";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertAirplane(Airplane airplane) {
        String sql = "INSERT INTO airplanes(weight, maxSpeed, wingSpan, maxAltitude) VALUES(?, ?, ?, ?);";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, airplane.getWeight());
            pstmt.setDouble(2, airplane.getMaxSpeed());
            pstmt.setDouble(3, airplane.getWingSpan());
            pstmt.setDouble(4, airplane.getMaxAltitude());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Airplane> fetchAllAirplanes() {
        List<Airplane> airplanes = new ArrayList<>();
        String sql = "SELECT weight, maxSpeed, wingSpan, maxAltitude FROM airplanes;";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                airplanes.add(new Airplane(
                        rs.getDouble("weight"),
                        rs.getDouble("maxSpeed"),
                        rs.getDouble("wingSpan"),
                        rs.getDouble("maxAltitude")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return airplanes;
    }
}

public class Main {
    public static void main(String[] args) {
        AirplaneHandler handler = new AirplaneHandler();
        List<Airplane> airplanes = handler.generateAirplanes(10);

        // Расчет средней скорости
        double averageSpeed = handler.calculateAverageSpeed(airplanes);
        System.out.println("Средняя скорость: " + averageSpeed);

        // Поиск самолета с максимальным весом
        Airplane heaviest = handler.findMaxWeightAirplane(airplanes);
        System.out.println("Самый тяжелый самолет: " + heaviest);

        // Работа с файлами
        try {
            handler.saveAirplanesToFile(airplanes, "airplanes.txt");
            List<Airplane> loadedAirplanes = handler.loadAirplanesFromFile("airplanes.txt");
            System.out.println("Загруженные самолеты: " + loadedAirplanes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Работа с базой данных
        DataBase db = new DataBase();
        db.createTable();
        for (Airplane airplane : airplanes) {
            db.insertAirplane(airplane);
        }
        List<Airplane> dbAirplanes = db.fetchAllAirplanes();
        System.out.println("Самолеты из базы данных: " + dbAirplanes);

        // Генерация графика
        createChart(airplanes);
    }

    // Метод для создания и отображения столбчатой диаграммы
    private static void createChart(List<Airplane> airplanes) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Заполняем данные для графика
        for (Airplane airplane : airplanes) {
            dataset.addValue(airplane.getWeight(), "Вес", "Самолет " + airplanes.indexOf(airplane));
        }

        // Создаем график
        JFreeChart chart = ChartFactory.createBarChart(
                "Вес самолетов", // Заголовок
                "Самолет", // Ось X
                "Вес (кг)", // Ось Y
                dataset // Данные
        );

        // Настроим панель для отображения графика
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }
}
