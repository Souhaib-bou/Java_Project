-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : mer. 25 fév. 2026 à 17:08
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.1.25

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `hirely_db`
--

-- --------------------------------------------------------

--
-- Structure de la table `forum_comment`
--

CREATE TABLE `forum_comment` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `author_id` bigint(20) NOT NULL,
  `content` text NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `moderated_by` bigint(20) DEFAULT NULL,
  `moderated_at` datetime DEFAULT NULL,
  `moderation_note` text DEFAULT NULL,
  `edited_at` datetime DEFAULT NULL,
  `edited_by` bigint(20) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `forum_comment`
--

INSERT INTO `forum_comment` (`id`, `post_id`, `author_id`, `content`, `status`, `moderated_by`, `moderated_at`, `moderation_note`, `edited_at`, `edited_by`, `created_at`, `updated_at`) VALUES
(4, 5, 1, 'Keep it 1 page, strong projects, quantified impact. Happy to review.', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:52', '2026-02-14 19:39:52'),
(5, 5, 2, 'Thanks Ali! I will update it and share results.', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:52', '2026-02-14 19:39:52'),
(6, 4, 2, 'This should be hidden until admin approves (PENDING).', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:52', '2026-02-14 19:39:52'),
(17, 7, 99, 'Only 5 new posts !!!', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-15 18:43:17', '2026-02-15 18:54:02'),
(18, 7, 2, 'Good News!', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-15 18:50:29', '2026-02-15 18:53:28'),
(20, 7, 1, 'Great!', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-15 19:02:19', '2026-02-15 19:05:01'),
(22, 7, 1, 'Great!', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-15 19:05:17', '2026-02-15 19:05:17'),
(23, 7, 1, 'Great!', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-15 19:05:28', '2026-02-15 19:05:28'),
(24, 7, 1, 'Great!', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-15 19:05:43', '2026-02-15 19:05:43'),
(26, 6, 1, 'no works just fine', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-16 17:28:39', '2026-02-16 17:28:39'),
(27, 5, 1, 'of course anytime !', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-02-17 09:24:12', '2026-02-17 09:24:12'),
(28, 5, 2, 'Thank you !', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-02-17 09:51:30', '2026-02-17 09:51:30'),
(29, 4, 1, 'I hate you!', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-02-22 09:22:46', '2026-02-22 09:22:46'),
(30, 4, 1, 'I like this post', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-22 09:23:01', '2026-02-22 09:23:01'),
(31, 6, 1, 'this so disgusting !', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-02-22 09:55:34', '2026-02-22 09:55:34'),
(32, 4, 1, 'shit', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-02-22 09:56:58', '2026-02-22 09:56:58'),
(33, 4, 1, 'I love this post', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-22 09:57:44', '2026-02-22 09:57:44'),
(34, 4, 1, 'need more info plz', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-22 09:57:55', '2026-02-22 09:57:55'),
(35, 6, 2, 'Kill yourself', 'REJECTED', NULL, NULL, NULL, NULL, NULL, '2026-02-24 09:14:27', '2026-02-24 09:14:27');

-- --------------------------------------------------------

--
-- Structure de la table `forum_notification`
--

CREATE TABLE `forum_notification` (
  `id` bigint(20) NOT NULL,
  `recipient_user_id` bigint(20) NOT NULL,
  `actor_user_id` bigint(20) DEFAULT NULL,
  `type` varchar(40) NOT NULL,
  `post_id` bigint(20) DEFAULT NULL,
  `comment_id` bigint(20) DEFAULT NULL,
  `message` varchar(255) NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `forum_post`
--

CREATE TABLE `forum_post` (
  `id` bigint(20) NOT NULL,
  `author_id` bigint(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `tag` varchar(100) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0,
  `is_locked` tinyint(1) NOT NULL DEFAULT 0,
  `moderated_by` bigint(20) DEFAULT NULL,
  `moderated_at` datetime DEFAULT NULL,
  `moderation_note` text DEFAULT NULL,
  `edited_at` datetime DEFAULT NULL,
  `edited_by` bigint(20) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `forum_post`
--

INSERT INTO `forum_post` (`id`, `author_id`, `title`, `content`, `tag`, `status`, `is_pinned`, `is_locked`, `moderated_by`, `moderated_at`, `moderation_note`, `edited_at`, `edited_by`, `created_at`, `updated_at`) VALUES
(3, 1, 'urgent', 'bitcoin value decreased', '#investment', 'APPROVED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-12 20:54:30', '2026-02-15 18:16:52'),
(4, 1, 'Internship Questions', 'Ask anything about internships here. Mohammed, drop your questions!', '#internships', 'APPROVED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:41', '2026-02-14 19:39:41'),
(5, 2, 'Need help with CV format', 'Any recommendations for a clean 1-page CV?', '#career', 'APPROVED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:41', '2026-02-14 19:39:41'),
(6, 2, 'Bug: comment refresh', 'Sometimes I need to press refresh twice to see new comments. Anyone else?', '#bugs', 'APPROVED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:41', '2026-02-14 19:39:41'),
(7, 1, 'New softoware developers posts', 'the company Itech is offering new posts', '#job', 'APPROVED', 1, 1, NULL, NULL, NULL, NULL, NULL, '2026-02-15 18:16:17', '2026-02-15 19:23:54'),
(9, 99, 'Welcome to Hirely', 'Dear users all content here should be professional !', '#Hirely', 'APPROVED', 1, 1, NULL, NULL, NULL, NULL, NULL, '2026-02-15 19:31:45', '2026-02-17 09:53:39'),
(11, 1, 'DS internships', '“Looking for a summer internship in data science. Any openings in Tunis?”', '#internships', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-23 17:16:08', '2026-02-23 17:16:08'),
(12, 1, 'DS internship', '“Looking for a summer internship in data science. Any openings in Tunis?”', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-23 17:29:54', '2026-02-23 17:29:54'),
(13, 1, 'DS internship', '“Looking for a summer internship in data science. Any openings in Tunis?”', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-23 17:37:34', '2026-02-23 17:37:34'),
(14, 1, 'DS internship', '“Looking for a summer internship in data science. Any openings in Tunis?”', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-23 17:42:27', '2026-02-23 17:42:27'),
(15, 1, 'DS internship', '“Looking for a summer internship in data science. Any openings in Tunis?”', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-23 18:00:43', '2026-02-23 18:00:43'),
(16, 1, 'DS internship', '“Looking for a summer internship in data science. Any openings in Tunis?”', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-23 18:10:39', '2026-02-23 18:10:39'),
(17, 1, 'internship', '“Looking for a summer internship in data science. Any openings in Tunis?”', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-23 18:18:49', '2026-02-23 18:18:49'),
(18, 1, 'Summer Internship (Data Science) — Tunis / Remote', 'Hi everyone, I’m looking for a summer internship in Data Science / Machine Learning in Tunis (or remote).\nI’m comfortable with Python, pandas, scikit-learn, and basic SQL, and I’ve built a small project using data cleaning + model training.\nIf your company is hiring interns (or if you know of openings), I’d really appreciate:\n\nthe role title\n\nrequired skills\n\napplication link / email\n\nwhether it’s remote/hybrid\n\nThanks in advance!', '#Internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 06:23:27', '2026-02-24 06:23:27'),
(19, 1, 'Summer Internship (Data Science) — Tunis / Remote', 'Hi everyone, I’m looking for a summer internship in Data Science / Machine Learning in Tunis (or remote).\nI’m comfortable with Python, pandas, scikit-learn, and basic SQL, and I’ve built a small project using data cleaning + model training.\nIf your company is hiring interns (or if you know of openings), I’d really appreciate:\n\nthe role title\n\nrequired skills\n\napplication link / email\n\nwhether it’s remote/hybrid\n\nThanks in advance!', '#Internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 06:28:53', '2026-02-24 06:28:53'),
(20, 1, 'DS internship', 'I\'m looking for internships in Tunis I\'m a student looking for remote ones', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 09:03:53', '2026-02-24 09:03:53'),
(21, 2, 'new jobs', 'Company Y is hiring 50 new jobs', '#job', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 09:24:55', '2026-02-24 09:24:55'),
(22, 1, 'New Jobs', 'Company YY is hiring 70 new jobs', '#job', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 10:02:14', '2026-02-24 10:02:14'),
(23, 2, 'Bug: comment refresh', 'Sometimes I need to press refresh twice to see new comments. Anyone else?', '#bugs', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 10:07:19', '2026-02-24 10:07:19'),
(24, 2, 'Data Science Internship – Summer 2026 (Tunis / Remote)', 'Hi everyone,\nI’m currently looking for a summer internship in Data Science or Machine Learning in Tunis (or remote).\nI have experience with Python, pandas, scikit-learn, and basic SQL. I’ve completed a small ML project involving data cleaning and model evaluation.\n\nIf your company is hiring interns, I’d appreciate any details about:\n\nRequired skills\n\nApplication link\n\nRemote/hybrid options\n\nThank you!', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 10:32:38', '2026-02-24 10:32:38'),
(25, 2, 'Data Science Internship – Summer 2026 (Tunis / Remote)', 'Hi everyone,\nI’m currently looking for a summer internship in Data Science or Machine Learning in Tunis (or remote).\nI have experience with Python, pandas, scikit-learn, and basic SQL. I’ve completed a small ML project involving data cleaning and model evaluation.\n\nIf your company is hiring interns, I’d appreciate any details about:\n\nRequired skills\n\nApplication link\n\nRemote/hybrid options\n\nThank you!', '#internship', 'PENDING', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 10:34:43', '2026-02-24 10:34:43'),
(26, 2, 'Fuck you', 'Fuck youFuck youFuck youFuck youFuck youFuck youFuck youFuck youFuck youFuck youFuck youFuck youFuck you', '#love', 'REJECTED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 10:37:19', '2026-02-24 10:37:19'),
(27, 2, 'nigger', 'nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger nigger', '#blm', 'REJECTED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-24 10:38:30', '2026-02-24 10:38:30');

-- --------------------------------------------------------

--
-- Structure de la table `forum_post_interaction`
--

CREATE TABLE `forum_post_interaction` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `type` varchar(10) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `role`
--

CREATE TABLE `role` (
  `role_id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'active',
  `default_dashboard` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `role`
--

INSERT INTO `role` (`role_id`, `name`, `description`, `status`, `default_dashboard`, `created_at`, `updated_at`) VALUES
(1, 'software_developer', 'Software Developer role', 'active', 'forum', '2026-02-12 17:29:57', '2026-02-12 17:29:57'),
(2, 'admin', 'Administrator role', 'active', 'admin', '2026-02-15 18:38:28', '2026-02-15 18:38:28');

-- --------------------------------------------------------

--
-- Structure de la table `user`
--

CREATE TABLE `user` (
  `user_id` bigint(20) NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role_id` int(11) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user`
--

INSERT INTO `user` (`user_id`, `first_name`, `last_name`, `email`, `password_hash`, `role_id`, `status`, `created_at`, `updated_at`) VALUES
(1, 'Ali', 'Ben Salah', 'ali@hirely.tn', 'dummy_hash', 1, 'active', '2026-02-12 17:30:12', '2026-02-12 17:30:12'),
(2, 'Mohammed', 'Rhim', 'mohammed@hirely.tn', 'dummy_hash', 1, 'active', '2026-02-14 19:39:24', '2026-02-15 18:06:45'),
(99, 'Admin', 'Hirely', 'admin@hirely.tn', 'dummy_hash', 2, 'active', '2026-02-15 18:38:50', '2026-02-15 18:38:50');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `forum_comment`
--
ALTER TABLE `forum_comment`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_forum_comment_moderated_by` (`moderated_by`),
  ADD KEY `fk_forum_comment_edited_by` (`edited_by`),
  ADD KEY `idx_forum_comment_post_created` (`post_id`,`created_at`),
  ADD KEY `idx_forum_comment_author` (`author_id`),
  ADD KEY `idx_forum_comment_created` (`created_at`);

--
-- Index pour la table `forum_notification`
--
ALTER TABLE `forum_notification`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_fn_recipient_read_created` (`recipient_user_id`,`is_read`,`created_at`),
  ADD KEY `idx_fn_post` (`post_id`),
  ADD KEY `idx_fn_comment` (`comment_id`),
  ADD KEY `fk_fn_actor_20260225` (`actor_user_id`);

--
-- Index pour la table `forum_post`
--
ALTER TABLE `forum_post`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_forum_post_moderated_by` (`moderated_by`),
  ADD KEY `fk_forum_post_edited_by` (`edited_by`),
  ADD KEY `idx_forum_post_feed` (`status`,`is_pinned`,`created_at`),
  ADD KEY `idx_forum_post_author` (`author_id`),
  ADD KEY `idx_forum_post_created` (`created_at`);

--
-- Index pour la table `forum_post_interaction`
--
ALTER TABLE `forum_post_interaction`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_fpi_post_user_type` (`post_id`,`user_id`,`type`),
  ADD KEY `idx_fpi_post_type` (`post_id`,`type`),
  ADD KEY `idx_fpi_user_type` (`user_id`,`type`);

--
-- Index pour la table `role`
--
ALTER TABLE `role`
  ADD PRIMARY KEY (`role_id`),
  ADD UNIQUE KEY `name` (`name`);

--
-- Index pour la table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_user_role` (`role_id`),
  ADD KEY `idx_user_status` (`status`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `forum_comment`
--
ALTER TABLE `forum_comment`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=36;

--
-- AUTO_INCREMENT pour la table `forum_notification`
--
ALTER TABLE `forum_notification`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `forum_post`
--
ALTER TABLE `forum_post`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=28;

--
-- AUTO_INCREMENT pour la table `forum_post_interaction`
--
ALTER TABLE `forum_post_interaction`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `role`
--
ALTER TABLE `role`
  MODIFY `role_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `user`
--
ALTER TABLE `user`
  MODIFY `user_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=100;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `forum_comment`
--
ALTER TABLE `forum_comment`
  ADD CONSTRAINT `fk_forum_comment_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_comment_edited_by` FOREIGN KEY (`edited_by`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_comment_moderated_by` FOREIGN KEY (`moderated_by`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_comment_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `forum_notification`
--
ALTER TABLE `forum_notification`
  ADD CONSTRAINT `fk_fn_actor_20260225` FOREIGN KEY (`actor_user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_fn_comment_20260225` FOREIGN KEY (`comment_id`) REFERENCES `forum_comment` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_fn_post_20260225` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_fn_recipient_20260225` FOREIGN KEY (`recipient_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `forum_post`
--
ALTER TABLE `forum_post`
  ADD CONSTRAINT `fk_forum_post_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_post_edited_by` FOREIGN KEY (`edited_by`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_post_moderated_by` FOREIGN KEY (`moderated_by`) REFERENCES `user` (`user_id`);

--
-- Contraintes pour la table `forum_post_interaction`
--
ALTER TABLE `forum_post_interaction`
  ADD CONSTRAINT `fk_fpi_post_20260225` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_fpi_user_20260225` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `user`
--
ALTER TABLE `user`
  ADD CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
