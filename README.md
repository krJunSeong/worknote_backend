# WorkNote

業務・学習内容を記録し、振り返りやスキル整理に活用するためのWebアプリケーションです。

日々の作業内容を蓄積し、後から「何を経験したか」「どの技術を使用したか」を確認しやすくすることを目的として開発しました。

🔗 **Demo**
https://worknote-ai.vercel.app/

---

## 📌 Overview

エンジニアとして日々経験した業務や学習内容は、時間が経つと細かい内容を忘れてしまうことがあります。

WorkNoteでは、日々の作業内容を簡単に記録・管理することで、

* 業務内容の振り返り
* 学習内容の整理
* 使用技術・経験の蓄積
* 職務経歴書や面接準備への活用

を行いやすくすることを目標としています。

---

## ✨ Features

### 👤 ユーザー管理

* ユーザー登録
* ログイン
* ユーザーごとのデータ管理

### 📝 Work Log

* 業務・学習内容の登録
* タイトル・詳細内容の保存
* 登録したWork Logの一覧表示
* ユーザー単位での記録管理

---

## 🛠 Tech Stack

### Frontend

![React](https://img.shields.io/badge/React-61DAFB?logo=react\&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-646CFF?logo=vite\&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?logo=javascript\&logoColor=black)
![Axios](https://img.shields.io/badge/Axios-5A29E4?logo=axios\&logoColor=white)

* React
* Vite
* JavaScript
* Axios

### Backend

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot\&logoColor=white)
![Java](https://img.shields.io/badge/Java-17+-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql\&logoColor=white)

* Spring Boot
* Java
* Spring Data JPA
* PostgreSQL
* REST API

### Deployment

![Vercel](https://img.shields.io/badge/Vercel-000000?logo=vercel\&logoColor=white)

* Frontend: Vercel
* Backend / Database: REST API連携

---

## 🏗 Architecture

```text
┌─────────────────────┐
│      React / Vite   │
│      Frontend       │
└──────────┬──────────┘
           │
           │ REST API
           │ Axios
           ▼
┌─────────────────────┐
│     Spring Boot     │
│      Backend        │
│                     │
│ Controller          │
│ Service             │
│ Repository          │
└──────────┬──────────┘
           │
           │ JPA / Hibernate
           ▼
┌─────────────────────┐
│     PostgreSQL      │
│      Database       │
└─────────────────────┘
```

---

## 🔄 Basic Flow

```text
ユーザー登録 / ログイン
        ↓
    WorkNote
        ↓
業務・学習内容を入力
        ↓
    REST API
        ↓
 Spring Boot
        ↓
   PostgreSQL
        ↓
記録一覧として表示
```

---

## 🚀 Getting Started

### 1. Clone

```bash
git clone <repository-url>
cd <repository-name>
```

### 2. Install dependencies

```bash
npm install
```

### 3. Run development server

```bash
npm run dev
```

Viteの開発サーバーを起動後、ブラウザからアクセスしてください。

---

## 💡 Development Purpose

本プロジェクトでは、単純な画面実装だけではなく、

* ReactとSpring Bootを利用したフロントエンド・バックエンド分離構成
* REST APIを利用したデータ通信
* PostgreSQLを利用したデータ管理
* ユーザーごとのデータ管理
* Webアプリケーションのデプロイ

など、実際のWebサービス開発を意識した構成で実装しています。

---

## 🔮 Future Improvements

今後、以下の機能追加・改善を予定しています。

* AIによる業務内容の要約
* 使用技術・技術タグの自動抽出
* 記録内容から面接想定質問を生成
* 認証・認可機能の改善
* UI / UXの改善

---

## 👨‍💻 Author

**JEONG JUNSEONG**

Webバックエンド開発を中心に学習・開発しています。
