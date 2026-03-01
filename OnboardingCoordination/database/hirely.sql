-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Feb 16, 2026 at 05:02 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `hirely`
--

-- --------------------------------------------------------

--
-- Table structure for table `application`
--

CREATE TABLE `application` (
  `applicationId` int(11) NOT NULL,
  `applicationDate` date DEFAULT NULL,
  `coverLetter` text DEFAULT NULL,
  `currentStatus` enum('Pending','Reviewed','Accepted','Rejected') DEFAULT 'Pending',
  `resumePath` varchar(255) DEFAULT NULL,
  `lastUpdateDate` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `user_id` int(11) DEFAULT NULL,
  `jobOfferId` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `application`
--

INSERT INTO `application` (`applicationId`, `applicationDate`, `coverLetter`, `currentStatus`, `resumePath`, `lastUpdateDate`, `user_id`, `jobOfferId`) VALUES
(2, '2024-02-07', 'azddazd', 'Pending', 'zad', '2026-02-16 15:36:33', 3, 2);

-- --------------------------------------------------------

--
-- Table structure for table `joboffer`
--

CREATE TABLE `joboffer` (
  `jobOfferId` int(11) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `contractType` enum('CDI','CDD','Internship','Freelance') DEFAULT NULL,
  `salary` decimal(10,2) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `experienceRequired` int(11) DEFAULT NULL,
  `publicationDate` date DEFAULT NULL,
  `status` enum('Open','Closed') DEFAULT 'Open',
  `user_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `joboffer`
--

INSERT INTO `joboffer` (`jobOfferId`, `title`, `description`, `contractType`, `salary`, `location`, `experienceRequired`, `publicationDate`, `status`, `user_id`) VALUES
(2, 'zef', 'zefz', 'CDD', 21.00, 'gzgz', 21, '2023-03-08', 'Open', 3),
(3, 'zef', 'azdzad', 'CDD', 21.00, 'zadazdazd', 21, '2025-02-06', 'Open', 3);

-- --------------------------------------------------------

--
-- Table structure for table `onboardingplan`
--

CREATE TABLE `onboardingplan` (
  `planId` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `status` varchar(50) DEFAULT NULL,
  `deadline` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `onboardingplan`
--

INSERT INTO `onboardingplan` (`planId`, `user_id`, `status`, `deadline`) VALUES
(6, 3, 'Completed', '2026-02-12'),
(7, 3, 'In Progress', '2026-02-27');

-- --------------------------------------------------------

--
-- Table structure for table `onboardingtask`
--

CREATE TABLE `onboardingtask` (
  `taskId` int(11) NOT NULL,
  `planId` int(11) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `deadline` date DEFAULT NULL,
  `filePath` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `role`
--

CREATE TABLE `role` (
  `role_id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'active',
  `default_dashboard` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `role`
--

INSERT INTO `role` (`role_id`, `name`, `description`, `status`, `default_dashboard`) VALUES
(1, 'active', 'ddddddd', 'active', 'ddddd'),
(2, 'recruiter', 'dddddddd', 'Active', 'ddddddddd');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'active',
  `profile_pic` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `first_name`, `last_name`, `email`, `password`, `role_id`, `status`, `profile_pic`) VALUES
(3, 'youssef', 'kaddech', 'youssef@gmail.com', '123123', 1, 'active', 'C:\\Users\\youss\\IdeaProjects\\OnboardingCoordination\\userpics\\user_3.png');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `application`
--
ALTER TABLE `application`
  ADD PRIMARY KEY (`applicationId`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `jobOfferId` (`jobOfferId`);

--
-- Indexes for table `joboffer`
--
ALTER TABLE `joboffer`
  ADD PRIMARY KEY (`jobOfferId`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `onboardingplan`
--
ALTER TABLE `onboardingplan`
  ADD PRIMARY KEY (`planId`),
  ADD KEY `userId` (`user_id`);

--
-- Indexes for table `onboardingtask`
--
ALTER TABLE `onboardingtask`
  ADD PRIMARY KEY (`taskId`),
  ADD KEY `planId` (`planId`);

--
-- Indexes for table `role`
--
ALTER TABLE `role`
  ADD PRIMARY KEY (`role_id`),
  ADD UNIQUE KEY `name` (`name`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `fk_users_role` (`role_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `application`
--
ALTER TABLE `application`
  MODIFY `applicationId` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `joboffer`
--
ALTER TABLE `joboffer`
  MODIFY `jobOfferId` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `onboardingplan`
--
ALTER TABLE `onboardingplan`
  MODIFY `planId` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `onboardingtask`
--
ALTER TABLE `onboardingtask`
  MODIFY `taskId` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `role`
--
ALTER TABLE `role`
  MODIFY `role_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `application`
--
ALTER TABLE `application`
  ADD CONSTRAINT `application_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `application_ibfk_2` FOREIGN KEY (`jobOfferId`) REFERENCES `joboffer` (`jobOfferId`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `joboffer`
--
ALTER TABLE `joboffer`
  ADD CONSTRAINT `joboffer_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `onboardingplan`
--
ALTER TABLE `onboardingplan`
  ADD CONSTRAINT `onboardingplan_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `onboardingtask`
--
ALTER TABLE `onboardingtask`
  ADD CONSTRAINT `onboardingtask_ibfk_1` FOREIGN KEY (`planId`) REFERENCES `onboardingplan` (`planId`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `fk_users_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
