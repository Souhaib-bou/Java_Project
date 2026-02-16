-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Feb 16, 2026 at 05:36 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.1.25

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `hirely_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `forum_comment`
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
-- Dumping data for table `forum_comment`
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
(26, 6, 1, 'no works just fine', 'APPROVED', NULL, NULL, NULL, NULL, NULL, '2026-02-16 17:28:39', '2026-02-16 17:28:39');

-- --------------------------------------------------------

--
-- Table structure for table `forum_post`
--

CREATE TABLE `forum_post` (
  `id` bigint(20) NOT NULL,
  `author_id` bigint(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `category` varchar(100) DEFAULT NULL,
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
-- Dumping data for table `forum_post`
--

INSERT INTO `forum_post` (`id`, `author_id`, `title`, `content`, `category`, `status`, `is_pinned`, `is_locked`, `moderated_by`, `moderated_at`, `moderation_note`, `edited_at`, `edited_by`, `created_at`, `updated_at`) VALUES
(3, 1, 'urgent', 'bitcoin value decreased', '#investment', 'APPROVED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-12 20:54:30', '2026-02-15 18:16:52'),
(4, 1, 'Internship Questions', 'Ask anything about internships here. Mohammed, drop your questions!', '#internships', 'APPROVED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:41', '2026-02-14 19:39:41'),
(5, 2, 'Need help with CV format', 'Any recommendations for a clean 1-page CV?', '#career', 'APPROVED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:41', '2026-02-14 19:39:41'),
(6, 2, 'Bug: comment refresh', 'Sometimes I need to press refresh twice to see new comments. Anyone else?', '#bugs', 'APPROVED', 0, 0, NULL, NULL, NULL, NULL, NULL, '2026-02-14 19:39:41', '2026-02-14 19:39:41'),
(7, 1, 'New softoware developers posts', 'the company Itech is offering new posts', '#job', 'APPROVED', 1, 1, NULL, NULL, NULL, NULL, NULL, '2026-02-15 18:16:17', '2026-02-15 19:23:54'),
(9, 99, 'Welcome to Hirely', 'Dear users all content here should be professional', '#Hirely', 'APPROVED', 1, 1, NULL, NULL, NULL, NULL, NULL, '2026-02-15 19:31:45', '2026-02-15 19:31:53');

-- --------------------------------------------------------

--
-- Table structure for table `role`
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
-- Dumping data for table `role`
--

INSERT INTO `role` (`role_id`, `name`, `description`, `status`, `default_dashboard`, `created_at`, `updated_at`) VALUES
(1, 'software_developer', 'Software Developer role', 'active', 'forum', '2026-02-12 17:29:57', '2026-02-12 17:29:57'),
(2, 'admin', 'Administrator role', 'active', 'admin', '2026-02-15 18:38:28', '2026-02-15 18:38:28');

-- --------------------------------------------------------

--
-- Table structure for table `user`
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
-- Dumping data for table `user`
--

INSERT INTO `user` (`user_id`, `first_name`, `last_name`, `email`, `password_hash`, `role_id`, `status`, `created_at`, `updated_at`) VALUES
(1, 'Ali', 'Ben Salah', 'ali@hirely.tn', 'dummy_hash', 1, 'active', '2026-02-12 17:30:12', '2026-02-12 17:30:12'),
(2, 'Mohammed', 'Rhim', 'mohammed@hirely.tn', 'dummy_hash', 1, 'active', '2026-02-14 19:39:24', '2026-02-15 18:06:45'),
(99, 'Admin', 'Hirely', 'admin@hirely.tn', 'dummy_hash', 2, 'active', '2026-02-15 18:38:50', '2026-02-15 18:38:50');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `forum_comment`
--
ALTER TABLE `forum_comment`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_forum_comment_moderated_by` (`moderated_by`),
  ADD KEY `fk_forum_comment_edited_by` (`edited_by`),
  ADD KEY `idx_forum_comment_post_created` (`post_id`,`created_at`),
  ADD KEY `idx_forum_comment_author` (`author_id`),
  ADD KEY `idx_forum_comment_created` (`created_at`);

--
-- Indexes for table `forum_post`
--
ALTER TABLE `forum_post`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_forum_post_moderated_by` (`moderated_by`),
  ADD KEY `fk_forum_post_edited_by` (`edited_by`),
  ADD KEY `idx_forum_post_feed` (`status`,`is_pinned`,`created_at`),
  ADD KEY `idx_forum_post_author` (`author_id`),
  ADD KEY `idx_forum_post_created` (`created_at`);

--
-- Indexes for table `role`
--
ALTER TABLE `role`
  ADD PRIMARY KEY (`role_id`),
  ADD UNIQUE KEY `name` (`name`);

--
-- Indexes for table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_user_role` (`role_id`),
  ADD KEY `idx_user_status` (`status`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `forum_comment`
--
ALTER TABLE `forum_comment`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT for table `forum_post`
--
ALTER TABLE `forum_post`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT for table `role`
--
ALTER TABLE `role`
  MODIFY `role_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `user`
--
ALTER TABLE `user`
  MODIFY `user_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=100;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `forum_comment`
--
ALTER TABLE `forum_comment`
  ADD CONSTRAINT `fk_forum_comment_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_comment_edited_by` FOREIGN KEY (`edited_by`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_comment_moderated_by` FOREIGN KEY (`moderated_by`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_comment_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `forum_post`
--
ALTER TABLE `forum_post`
  ADD CONSTRAINT `fk_forum_post_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_post_edited_by` FOREIGN KEY (`edited_by`) REFERENCES `user` (`user_id`),
  ADD CONSTRAINT `fk_forum_post_moderated_by` FOREIGN KEY (`moderated_by`) REFERENCES `user` (`user_id`);

--
-- Constraints for table `user`
--
ALTER TABLE `user`
  ADD CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
