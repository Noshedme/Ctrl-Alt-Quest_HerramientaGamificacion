# 🎨 GUÍA RÁPIDA DE ESTILOS - APLICAR EN NUEVOS COMPONENTES

## Referencia Rápida de Clases CSS Disponibles

### 📌 BOTONES

```xml
<!-- Botón primario (dorado) -->
<Button text="Ejecutar" styleClass="btn-primary" />

<!-- Botón secundario (púrpura) -->
<Button text="Opción" styleClass="btn-secondary" />

<!-- Botón transparente -->
<Button text="Enlace" styleClass="btn-ghost" />

<!-- Botón de icono -->
<Button text="⚙" styleClass="btn-icon" />

<!-- Botón peligroso -->
<Button text="Eliminar" styleClass="btn-danger" />
```

### 📋 PANELES Y CONTENEDORES

```xml
<!-- Panel principal con borde dorado -->
<VBox styleClass="panel-main">
    <Label text="Contenido" />
</VBox>

<!-- Tarjeta estándar -->
<VBox styleClass="panel-card">
    <Label text="Info" />
</VBox>

<!-- Tarjeta con acento púrpura -->
<VBox styleClass="panel-accent">
    <Label text="Especial" />
</VBox>

<!-- Sidebar (fondo translúcido) -->
<VBox styleClass="panel-sidebar">
    <Label text="Menu" />
</VBox>

<!-- Tarjeta de evento (gradiente púrpura) -->
<VBox styleClass="card-event">
    <Label text="Evento" />
</VBox>

<!-- Tarjeta de logro -->
<VBox styleClass="card-achievement">
    <Label text="🏆 Logro" />
</VBox>

<!-- Tarjeta con gradiente dorado -->
<VBox styleClass="panel-card-golden">
    <Label text="Premium" />
</VBox>

<!-- Tarjeta de misión (gradiente verde) -->
<VBox styleClass="card-mission">
    <Label text="📜 Misión" />
</VBox>

<!-- Tarjeta de tienda (gradiente púrpura) -->
<VBox styleClass="card-store-item">
    <Label text="💎 Ítem" />
</VBox>
```

### 📝 TÍTULOS Y ETIQUETAS

```xml
<!-- Título hero (grande, dorado con dropshadow) -->
<Label text="BIENVENIDO" styleClass="title-hero" />

<!-- Título de sección -->
<Label text="CONFIGURACIÓN" styleClass="title-section" />

<!-- Subtítulo -->
<Label text="Opciones" styleClass="title-subsection" />

<!-- Pequeño -->
<Label text="Opción" styleClass="title-small" />

<!-- Rúnico especial (con brillo) -->
<Label text="ESPECIAL" styleClass="title-runic" />

<!-- Texto primario -->
<Label text="Normal" styleClass="text-primary" />

<!-- Texto secundario (púrpura claro) -->
<Label text="Secundario" styleClass="text-secondary" />

<!-- Texto muted (gris) -->
<Label text="Muted" styleClass="text-muted" />

<!-- Texto con acento (púrpura bold) -->
<Label text="ESPECIAL" styleClass="text-accent" />

<!-- Texto rojo peligro -->
<Label text="ERROR" styleClass="text-danger" />

<!-- Texto verde éxito -->
<Label text="OK" styleClass="text-success" />
```

### 🏷️ ETIQUETAS (TAGS)

```xml
<!-- Etiqueta de estado (púrpura) -->
<Label text="Activo" styleClass="tag-status" />

<!-- Etiqueta dorada -->
<Label text="Premium" styleClass="tag-gold" />
```

### 🔤 CAMPOS DE ENTRADA

```xml
<!-- Campo de texto estilizado -->
<TextField styleClass="input-field" />

<!-- Campo enfocado automáticamente get dorado glow -->
<TextField styleClass="input-field" promptText="Escribe..." />

<!-- Etiqueta de campo -->
<Label text="NOMBRE" styleClass="input-label" />

<!-- Campo de error -->
<TextField styleClass="input-field input-error" />
```

### ⏳ BARRAS DE PROGRESO

```xml
<!-- Barra de progreso dorada -->
<ProgressBar progress="0.5" styleClass="progress-bar-gold" />

<!-- Barra de progreso púrpura -->
<ProgressBar progress="0.75" styleClass="progress-bar-purple" />

<!-- Barra de progreso verde -->
<ProgressBar progress="1.0" styleClass="progress-bar-green" />
```

### ✨ EFECTOS ESPECIALES

```xml
<!-- Panel con brillo dorado -->
<VBox styleClass="panel-card glow-gold">
    <Label text="Dorado" />
</VBox>

<!-- Panel con brillo púrpura -->
<VBox styleClass="panel-card glow-purple">
    <Label text="Púrpura" />
</VBox>

<!-- Panel con brillo rojo peligro -->
<VBox styleClass="panel-card glow-danger">
    <Label text="Peligro" />
</VBox>
```

### 🎬 AVATARES Y ELEMENTOS CIRCULARES

```xml
<!-- Avatar grande (radial gradient púrpura) -->
<StackPane styleClass="avatar-large">
    <ImageView fitWidth="80" fitHeight="80" />
</StackPane>

<!-- Avatar medio -->
<StackPane styleClass="avatar-medium">
    <ImageView fitWidth="50" fitHeight="50" />
</StackPane>

<!-- Avatar pequeño -->
<StackPane styleClass="avatar-small">
    <ImageView fitWidth="30" fitHeight="30" />
</StackPane>
```

### 📊 MODALES Y DIÁLOGOS

```xml
<!-- Panel modal elegante -->
<VBox styleClass="modal-panel">
    <Label text="Confirmación" styleClass="title-section" />
    <Label text="¿Continuar?" styleClass="text-primary" />
</VBox>

<!-- Overlay de carga -->
<VBox styleClass="loading-overlay">
    <ProgressIndicator />
    <Label text="Cargando..." styleClass="text-secondary" />
</VBox>
```

---

## 🎯 EJEMPLO COMPLETO

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import java.net.URL?>

<VBox xmlns="http://javafx.com/javafx/17" xmlns:fx="http://javafx.com/fxml/1" 
      spacing="20" style="-fx-background-color: transparent;">
    
    <stylesheets>
        <URL value="@/styles/shared.css" />
        <URL value="@/styles/home.css" />
    </stylesheets>
    
    <!-- Encabezado -->
    <HBox spacing="20" alignment="CENTER_LEFT">
        <Label text="Mi Panel" styleClass="title-section" />
        <Region HBox.hgrow="ALWAYS" />
        <Button text="✓" styleClass="btn-primary" />
    </HBox>
    
    <!-- Contenido -->
    <VBox styleClass="panel-card" spacing="15">
        <Label text="Opción" styleClass="input-label" />
        <TextField styleClass="input-field" promptText="Escribe..." />
        
        <HBox spacing="10">
            <Label text="Estado:" styleClass="text-secondary" />
            <Label text="Activo" styleClass="tag-status" />
        </HBox>
        
        <ProgressBar progress="0.65" styleClass="progress-bar-purple" />
    </VBox>
    
    <!-- Tarjetas -->
    <HBox spacing="15">
        <VBox styleClass="card-mission" HBox.hgrow="ALWAYS">
            <Label text="📜 Misión" styleClass="text-accent" />
            <Label text="Completa el tutorial" styleClass="text-primary" />
        </VBox>
        
        <VBox styleClass="card-achievement" HBox.hgrow="ALWAYS">
            <Label text="🏆 Logro" styleClass="text-accent" />
            <Label text="Primer login" styleClass="text-primary" />
        </VBox>
    </HBox>
    
</VBox>
```

---

## 💾 REFERENCIAS DE COLORES

Para uso en estilos inline si es necesario:

```css
/* Texto */
-fx-text-fill: #f7d27a;   /* Dorado primario */
-fx-text-fill: #ffffff;    /* Blanco */
-fx-text-fill: #b5a7c6;    /* Púrpura claro */
-fx-text-fill: #888888;    /* Gris muted */

/* Fondos */
-fx-background-color: #0d0915;    /* Fondo base */
-fx-background-color: #1a0f26;    /* Fondo oscuro */
-fx-background-color: #1e162d;    /* Fondo tarjeta */

/* Bordes */
-fx-border-color: rgba(247, 210, 122, 0.3);     /* Dorado translúcido */
-fx-border-color: rgba(138, 43, 226, 0.3);      /* Púrpura translúcido */

/* Gradientes */
linear-gradient(to bottom, #f7d27a, #d4a017)    /* Dorado */
linear-gradient(to bottom, #8a2be2, #a335ee)    /* Púrpura */
```

---

## 🔄 PATRONES COMUNES

### Para una Tarjeta Interactiva:
```xml
<VBox styleClass="card-item">
    <Label text="Título" styleClass="title-subsection" />
    <Separator />
    <Label text="Información" styleClass="text-secondary" />
</VBox>
```

### Para un Botón Acción Principal:
```xml
<Button text="EJECUTAR" styleClass="btn-primary" maxWidth="Infinity" />
```

### Para un Menú Lateral:
```xml
<VBox styleClass="panel-sidebar" prefWidth="250" spacing="10">
    <Button text="🏠 Inicio" styleClass="btn-nav" />
    <Button text="📜 Misiones" styleClass="btn-nav" />
</VBox>
```

---

**Última Actualización:** 2 de Marzo de 2026
