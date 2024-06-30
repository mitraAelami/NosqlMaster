package qengine.program;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Histogramme extends JFrame {

    public Histogramme(String title, List<Integer> data) {
        super(title);

        // Convertir la liste en tableau d'entiers
        int[] dataArray = data.stream().mapToInt(Integer::intValue).toArray();

        // Creer un jeu de donnees pour l'histogramme
        CategoryDataset dataset = createDataset(dataArray);

        // Creer le graphique a barres
        JFreeChart chart = ChartFactory.createBarChart(
                title,
                "Reponses",              // Axe des X
                "Occurrence",          // Axe des Y
                dataset
        );

        // Ajouter le graphique a une interface graphique Swing
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private CategoryDataset createDataset(int[] data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Calculer les valeurs et occurrences
        Map<Integer, Integer> histogram = calculateHistogram(data);

        // Ajouter les donnees au jeu de donnees
        for (int value : histogram.keySet()) {
            dataset.addValue(histogram.get(value), "Occurrence", String.valueOf(value));
        }

        return dataset;
    }

    private Map<Integer, Integer> calculateHistogram(int[] data) {
        Map<Integer, Integer> histogram = new HashMap<>();

        for (int value : data) {
            histogram.put(value, histogram.getOrDefault(value, 0) + 1);
        }

        return histogram;
    }
}
