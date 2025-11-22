# 🎮 Infinity Live: Gamifica tu Crecimiento Personal

<div align="center">

![Versión](https://img.shields.io/badge/versión-1.0.0-blue.svg)
![Plataforma](https://img.shields.io/badge/plataforma-Android-green.svg)
![Licencia](https://img.shields.io/badge/licencia-MIT-orange.svg)
![Firebase](https://img.shields.io/badge/Firebase-Realtime-yellow.svg)

Una aplicación móvil que transforma la disciplina personal y la gestión de hábitos en una experiencia de juego de rol (RPG).

[Características](#-características) • [Instalación](#-instalación) • [Arquitectura](#️-arquitectura) • [Uso](#-uso) • [Contribuir](#-contribuir)

</div>

---

## 💡 Concepto Principal

**Infinity Live** te permite crear un avatar cuya **Vida (HP)** representa tu bienestar integral. El progreso se mide en **Áreas Clave** (como Salud, Finanzas o Espiritualidad), que suben de nivel al completar **Hábitos Positivos** (ganando XP y Monedas) y disminuyen al caer en **Hábitos Negativos** (perdiendo HP).

> La aplicación está diseñada para forzar la autoconciencia y la responsabilidad, convirtiendo los errores en oportunidades de mejora con consecuencias reales.

---

## ✨ Características

### 🎯 Sistema de Gamificación

- **Avatar Personalizado**: Tu personaje refleja tu progreso en tiempo real
- **Sistema de HP**: Vida que representa tu bienestar integral
- **XP y Niveles**: Las áreas de vida suben de nivel al completar hábitos
- **Monedas Virtuales**: Sistema de recompensas por hábitos positivos
- **Mecánica de Muerte**: Cuando HP ≤ 0, la app se bloquea hasta completar un "Castigo Consciente"

### 📊 Gestión de Hábitos

- ✅ **Hábitos Positivos**: Gana XP, Monedas y sube de nivel
- ❌ **Hábitos Negativos**: Pierdes HP y enfrentas consecuencias
- 🎯 **Áreas Personalizables**: Define entre 7 y 12 áreas de vida
- 📈 **Progreso en Tiempo Real**: Sincronización instantánea con la nube

### 🔐 Autenticación y Perfil

- Registro e inicio de sesión con Email/Contraseña
- Integración con Google Sign-In
- Sesión persistente entre reinicios
- Foto de perfil y biografía personalizables
- Almacenamiento CRUD local para datos de perfil

---

## 🛠️ Arquitectura

### Stack Tecnológico

| Tecnología | Propósito | Tipo |
|------------|-----------|------|
| **Kotlin** | Lenguaje de programación principal | Frontend/Lógica |
| **Firebase Auth** | Manejo de sesiones de usuario (UID) | Cloud |
| **Cloud Firestore** | Almacenamiento en tiempo real de Hábitos, Áreas, Monedas y HP | Cloud |
| **Firebase UI** | Adaptadores (`FirestoreRecyclerAdapter`) para listas en tiempo real | Integración |
| **SQLite** | Almacenamiento local de Foto de Perfil y Biografía | Local |
| **Glide** | Carga eficiente de imágenes desde almacenamiento local | Librería |
| **ViewBinding** | Acceso seguro a las vistas | Librería |

### Arquitectura Híbrida

```
┌─────────────────────────────────────────┐
│         INFINITY LIVE APP               │
├─────────────────────────────────────────┤
│  ┌───────────┐      ┌───────────────┐  │
│  │  Firebase │◄────►│   Firestore   │  │
│  │   Auth    │      │  (Hábitos/XP) │  │
│  └───────────┘      └───────────────┘  │
│       ▲                     ▲           │
│       │                     │           │
│  ┌────┴─────────────────────┴────┐     │
│  │      BaseActivity Pattern      │     │
│  │  (Menú lateral + Lifecycle)   │     │
│  └────────────┬───────────────────┘     │
│               │                         │
│  ┌────────────▼───────────────────┐    │
│  │   SQLite (PerfilDbHelper)      │    │
│  │   (Perfil local + Biografía)   │    │
│  └────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

---

## 🚀 Instalación

### Requisitos Previos

- Android Studio Arctic Fox o superior
- JDK 11+
- Cuenta de Firebase con proyecto configurado
- Gradle 7.0+

### Configuración

1. **Clona el repositorio**
   ```bash
   git clone https://github.com/tuusuario/infinity-live.git
   cd infinity-live
   ```

2. **Configura Firebase**
   - Crea un proyecto en [Firebase Console](https://console.firebase.google.com/)
   - Descarga el archivo `google-services.json`
   - Colócalo en `app/google-services.json`
   - Habilita Authentication (Email/Password y Google)
   - Crea una base de datos Cloud Firestore

3. **Estructura de Firestore**
   ```
   users/
     └── {userId}/
           ├── perfil (documento)
           │     ├── nombre: String
           │     ├── hp: Number
           │     └── monedas: Number
           ├── areas/ (colección)
           │     └── {areaId}
           │           ├── nombre: String
           │           ├── nivel: Number
           │           └── xp: Number
           └── habitos/ (colección)
                 └── {habitoId}
                       ├── nombre: String
                       ├── tipo: String (+/-)
                       └── valor: Number
   ```

4. **Sincroniza y Compila**
   ```bash
   ./gradlew build
   ```

5. **Ejecuta la aplicación**
   - Conecta un dispositivo Android o inicia un emulador
   - Click en "Run" en Android Studio

---

## 📱 Uso

### Primera Configuración

1. **Registro**: Crea una cuenta con email/contraseña o Google
2. **Configuración Inicial**: Define tus áreas de vida (5-12 áreas)
3. **Crea tu Avatar**: Personaliza tu foto de perfil y biografía
4. **Agrega Hábitos**: Crea hábitos positivos y negativos

### Flujo de Juego

```
┌──────────────────────────────────────────────┐
│  Completar Hábito Positivo                   │
│  ↓                                            │
│  +XP → Subir Nivel de Área                   │
│  +Monedas → Recompensas                      │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│  Registrar Hábito Negativo                   │
│  ↓                                            │
│  -HP → Si HP ≤ 0: MUERTE                     │
│  ↓                                            │
│  App Bloqueada → RevivirActivity             │
│  ↓                                            │
│  Castigo Consciente → -10% Monedas           │
│  ↓                                            │
│  Confirmar Cumplimiento → Restaurar HP       │
└──────────────────────────────────────────────┘
```

### Características Principales

#### 🎯 Gestión de Áreas
- Accede desde el menú lateral
- Visualiza progreso de cada área
- Monitorea XP y nivel actual

#### 📝 Registro de Hábitos
- Marca hábitos completados diariamente
- Visualiza impacto inmediato en HP/XP
- Historial de actividades

#### 💀 Sistema de Muerte
- La app se bloquea al llegar a HP ≤ 0
- Debes completar un reto físico/mental
- Pérdida del 10% de monedas como castigo
- Restauración de HP tras confirmación

---

## 📊 Estado de Implementación

| Módulo | Característica | Estado |
|--------|----------------|--------|
| Autenticación | Login/Registro con Email y Contraseña | ✅ Completo |
| Autenticación | Opción "Continuar con Google" | ✅ Completo |
| Sesión | Persistencia entre reinicios | ✅ Completo |
| Configuración | Definir 3-12 áreas de vida dinámicamente | ✅ Completo |
| Sistema de Juego | Hábitos (+) otorgan XP/Monedas | ✅ Completo |
| Progreso | Áreas suben de nivel al alcanzar XP | ✅ Completo |
| Mecánica de Muerte | App se bloquea al HP ≤ 0 | ✅ Completo |
| Castigo | Pérdida 10% Monedas + Reto obligatorio | ✅ Completo |
| Perfil (CRUD) | Foto de perfil y biografía | ✅ Completo |
| Navegación | Menú lateral dinámico con áreas | ✅ Completo |

---

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Para contribuir:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

### Guías de Contribución

- Sigue las convenciones de código Kotlin
- Documenta funciones públicas
- Añade tests para nuevas features
- Actualiza el README si es necesario

---

## 📝 Roadmap

- [ ] Sistema de logros y badges
- [ ] Estadísticas y gráficos de progreso
- [ ] Modo multijugador/competitivo
- [ ] Tienda de items con monedas
- [ ] Notificaciones push para recordatorios
- [ ] Widget para pantalla de inicio
- [ ] Exportar/Importar datos
- [ ] Temas personalizables

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

---

## 👥 Autores

- **Jeferson Valencia** - *frontend* - [SamaelBlossom](https://github.com/SamaelBlossom)
- **Johan Suarez** - *backend* - [B Cio](https://github.com/Bcio01)

---

## 🙏 Agradecimientos

- Firebase por la infraestructura backend
- Comunidad de Kotlin por las mejores prácticas
- A todos los que creen en el poder de la gamificación para el crecimiento personal

---

<div align="center">

**¿Te gusta el proyecto? ¡Dale una ⭐ en GitHub!**

</div>
