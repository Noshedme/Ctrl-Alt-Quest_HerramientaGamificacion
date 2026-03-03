package com.ctrlaltquest.ui.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Duration;

/**
 * Gestor centralizado de animaciones para transiciones suaves entre vistas.
 * Proporciona varios tipos de transiciones con duración y efectos personalizables.
 */
public class AnimationManager {
    
    // Duración por defecto de las transiciones (en milisegundos)
    public static final double DEFAULT_FADE_DURATION = 400;
    public static final double DEFAULT_SLIDE_DURATION = 500;
    public static final double DEFAULT_SCALE_DURATION = 300;
    
    // --- TRANSICIONES DE ENTRADA ---
    
    /**
     * Transición de entrada por desvanecimiento (fade in)
     * El elemento aparece gradualmente desde opacidad 0 a 1
     */
    public static void fadeIn(Node node) {
        fadeIn(node, DEFAULT_FADE_DURATION);
    }
    
    public static void fadeIn(Node node, double durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
    
    /**
     * Transición de entrada con slide (deslizamiento desde la izquierda)
     */
    public static void slideInFromLeft(Node node) {
        slideInFromLeft(node, DEFAULT_SLIDE_DURATION);
    }
    
    public static void slideInFromLeft(Node node, double durationMs) {
        node.setOpacity(0);
        node.setTranslateX(-node.getBoundsInLocal().getWidth());
        
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromX(-node.getBoundsInLocal().getWidth());
        slide.setToX(0);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }
    
    /**
     * Transición de entrada con slide (deslizamiento desde la derecha)
     */
    public static void slideInFromRight(Node node) {
        slideInFromRight(node, DEFAULT_SLIDE_DURATION);
    }
    
    public static void slideInFromRight(Node node, double durationMs) {
        node.setOpacity(0);
        node.setTranslateX(node.getBoundsInLocal().getWidth());
        
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromX(node.getBoundsInLocal().getWidth());
        slide.setToX(0);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }
    
    /**
     * Transición de entrada con slide (deslizamiento desde arriba)
     */
    public static void slideInFromTop(Node node) {
        slideInFromTop(node, DEFAULT_SLIDE_DURATION);
    }
    
    public static void slideInFromTop(Node node, double durationMs) {
        node.setOpacity(0);
        node.setTranslateY(-node.getBoundsInLocal().getHeight());
        
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromY(-node.getBoundsInLocal().getHeight());
        slide.setToY(0);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }
    
    /**
     * Transición de entrada con slide (deslizamiento desde abajo)
     */
    public static void slideInFromBottom(Node node) {
        slideInFromBottom(node, DEFAULT_SLIDE_DURATION);
    }
    
    public static void slideInFromBottom(Node node, double durationMs) {
        node.setOpacity(0);
        node.setTranslateY(node.getBoundsInLocal().getHeight());
        
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromY(node.getBoundsInLocal().getHeight());
        slide.setToY(0);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }
    
    /**
     * Transición de entrada con escala (zoom in)
     */
    public static void zoomIn(Node node) {
        zoomIn(node, DEFAULT_SCALE_DURATION);
    }
    
    public static void zoomIn(Node node, double durationMs) {
        node.setOpacity(0);
        node.setScaleX(0.5);
        node.setScaleY(0.5);
        
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMs), node);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1);
        scale.setToY(1);
        
        ParallelTransition parallel = new ParallelTransition(fade, scale);
        parallel.play();
    }
    
    // --- TRANSICIONES DE SALIDA ---
    
    /**
     * Transición de salida por desvanecimiento (fade out)
     * El elemento desaparece gradualmente desde opacidad 1 a 0
     */
    public static void fadeOut(Node node) {
        fadeOut(node, DEFAULT_FADE_DURATION, null);
    }
    
    public static void fadeOut(Node node, double durationMs) {
        fadeOut(node, durationMs, null);
    }
    
    public static void fadeOut(Node node, double durationMs, Runnable onFinished) {
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(1);
        ft.setToValue(0);
        if (onFinished != null) {
            ft.setOnFinished(e -> onFinished.run());
        }
        ft.play();
    }
    
    /**
     * Transición de salida con slide (deslizamiento hacia la izquierda)
     */
    public static void slideOutToLeft(Node node) {
        slideOutToLeft(node, DEFAULT_SLIDE_DURATION, null);
    }
    
    public static void slideOutToLeft(Node node, double durationMs) {
        slideOutToLeft(node, durationMs, null);
    }
    
    public static void slideOutToLeft(Node node, double durationMs, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(1);
        fade.setToValue(0);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setToX(-node.getBoundsInLocal().getWidth());
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        if (onFinished != null) {
            parallel.setOnFinished(e -> onFinished.run());
        }
        parallel.play();
    }
    
    /**
     * Transición de salida con slide (deslizamiento hacia la derecha)
     */
    public static void slideOutToRight(Node node) {
        slideOutToRight(node, DEFAULT_SLIDE_DURATION, null);
    }
    
    public static void slideOutToRight(Node node, double durationMs) {
        slideOutToRight(node, durationMs, null);
    }
    
    public static void slideOutToRight(Node node, double durationMs, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(1);
        fade.setToValue(0);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setToX(node.getBoundsInLocal().getWidth());
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        if (onFinished != null) {
            parallel.setOnFinished(e -> onFinished.run());
        }
        parallel.play();
    }
    
    /**
     * Transición de salida con zoom out
     */
    public static void zoomOut(Node node) {
        zoomOut(node, DEFAULT_SCALE_DURATION, null);
    }
    
    public static void zoomOut(Node node, double durationMs) {
        zoomOut(node, durationMs, null);
    }
    
    public static void zoomOut(Node node, double durationMs, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(1);
        fade.setToValue(0);
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMs), node);
        scale.setToX(0.5);
        scale.setToY(0.5);
        
        ParallelTransition parallel = new ParallelTransition(fade, scale);
        if (onFinished != null) {
            parallel.setOnFinished(e -> onFinished.run());
        }
        parallel.play();
    }
    
    // --- TRANSICIONES COMBINADAS (ENTRADA + SALIDA) ---
    
    /**
     * Crea una transición que desvanece la vista antigua y desvanece la nueva
     */
    public static void crossFade(Node oldNode, Node newNode, double durationMs) {
        newNode.setOpacity(0);
        
        fadeOut(oldNode, durationMs, () -> {
            if (oldNode.getParent() != null) {
                ((Parent) oldNode.getParent()).getChildrenUnmodifiable().remove(oldNode);
            }
        });
        
        fadeIn(newNode, durationMs);
    }
    
    /**
     * Transición de página: salida a la derecha, entrada desde la izquierda
     */
    public static void pageTransitionNext(Node oldNode, Node newNode, double durationMs) {
        newNode.setOpacity(0);
        newNode.setTranslateX(newNode.getBoundsInLocal().getWidth());
        
        slideOutToRight(oldNode, durationMs, () -> {
            if (oldNode.getParent() != null) {
                ((Parent) oldNode.getParent()).getChildrenUnmodifiable().remove(oldNode);
            }
        });
        
        slideInFromRight(newNode, durationMs);
    }
    
    /**
     * Transición de página: salida a la izquierda, entrada desde la derecha
     */
    public static void pageTransitionPrevious(Node oldNode, Node newNode, double durationMs) {
        newNode.setOpacity(0);
        newNode.setTranslateX(-newNode.getBoundsInLocal().getWidth());
        
        slideOutToLeft(oldNode, durationMs, () -> {
            if (oldNode.getParent() != null) {
                ((Parent) oldNode.getParent()).getChildrenUnmodifiable().remove(oldNode);
            }
        });
        
        slideInFromLeft(newNode, durationMs);
    }
}
