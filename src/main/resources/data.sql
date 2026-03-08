INSERT INTO questions (text, difficulty)
SELECT 'What is the capital of France?',             1 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'What is the capital of France?');
INSERT INTO questions (text, difficulty)
SELECT 'How many sides does a hexagon have?',         1 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'How many sides does a hexagon have?');
INSERT INTO questions (text, difficulty)
SELECT 'Which planet is closest to the Sun?',         1 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'Which planet is closest to the Sun?');
INSERT INTO questions (text, difficulty)
SELECT 'Who wrote "Romeo and Juliet"?',               1 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'Who wrote "Romeo and Juliet"?');
INSERT INTO questions (text, difficulty)
SELECT 'What is the chemical symbol for gold?',       2 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'What is the chemical symbol for gold?');
INSERT INTO questions (text, difficulty)
SELECT 'In what year did World War II end?',          2 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'In what year did World War II end?');
INSERT INTO questions (text, difficulty)
SELECT 'What is the largest ocean on Earth?',         1 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'What is the largest ocean on Earth?');
INSERT INTO questions (text, difficulty)
SELECT 'Which element has the atomic number 1?',      2 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'Which element has the atomic number 1?');
INSERT INTO questions (text, difficulty)
SELECT 'How many bones are in the adult human body?', 2 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'How many bones are in the adult human body?');
INSERT INTO questions (text, difficulty)
SELECT 'What is the square root of 144?',             1 WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'What is the square root of 144?');

-- Q1: capital of France
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q1A1', id, 'Paris',  true,  0 FROM questions WHERE text = 'What is the capital of France?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q1A2', id, 'Berlin', false, 1 FROM questions WHERE text = 'What is the capital of France?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q1A3', id, 'Madrid', false, 2 FROM questions WHERE text = 'What is the capital of France?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q1A4', id, 'Rome',   false, 3 FROM questions WHERE text = 'What is the capital of France?' ON CONFLICT (id) DO NOTHING;

-- Q2: hexagon sides
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q2A1', id, '5', false, 0 FROM questions WHERE text = 'How many sides does a hexagon have?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q2A2', id, '6', true,  1 FROM questions WHERE text = 'How many sides does a hexagon have?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q2A3', id, '7', false, 2 FROM questions WHERE text = 'How many sides does a hexagon have?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q2A4', id, '8', false, 3 FROM questions WHERE text = 'How many sides does a hexagon have?' ON CONFLICT (id) DO NOTHING;

-- Q3: closest planet to the Sun
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q3A1', id, 'Venus',   false, 0 FROM questions WHERE text = 'Which planet is closest to the Sun?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q3A2', id, 'Mars',    false, 1 FROM questions WHERE text = 'Which planet is closest to the Sun?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q3A3', id, 'Mercury', true,  2 FROM questions WHERE text = 'Which planet is closest to the Sun?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q3A4', id, 'Earth',   false, 3 FROM questions WHERE text = 'Which planet is closest to the Sun?' ON CONFLICT (id) DO NOTHING;

-- Q4: Romeo and Juliet
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q4A1', id, 'Charles Dickens',     false, 0 FROM questions WHERE text = 'Who wrote "Romeo and Juliet"?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q4A2', id, 'William Shakespeare', true,  1 FROM questions WHERE text = 'Who wrote "Romeo and Juliet"?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q4A3', id, 'Jane Austen',         false, 2 FROM questions WHERE text = 'Who wrote "Romeo and Juliet"?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q4A4', id, 'Leo Tolstoy',         false, 3 FROM questions WHERE text = 'Who wrote "Romeo and Juliet"?' ON CONFLICT (id) DO NOTHING;

-- Q5: chemical symbol for gold
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q5A1', id, 'Ag', false, 0 FROM questions WHERE text = 'What is the chemical symbol for gold?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q5A2', id, 'Go', false, 1 FROM questions WHERE text = 'What is the chemical symbol for gold?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q5A3', id, 'Gd', false, 2 FROM questions WHERE text = 'What is the chemical symbol for gold?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q5A4', id, 'Au', true,  3 FROM questions WHERE text = 'What is the chemical symbol for gold?' ON CONFLICT (id) DO NOTHING;

-- Q6: end of WWII
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q6A1', id, '1943', false, 0 FROM questions WHERE text = 'In what year did World War II end?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q6A2', id, '1944', false, 1 FROM questions WHERE text = 'In what year did World War II end?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q6A3', id, '1945', true,  2 FROM questions WHERE text = 'In what year did World War II end?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q6A4', id, '1946', false, 3 FROM questions WHERE text = 'In what year did World War II end?' ON CONFLICT (id) DO NOTHING;

-- Q7: largest ocean
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q7A1', id, 'Atlantic Ocean', false, 0 FROM questions WHERE text = 'What is the largest ocean on Earth?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q7A2', id, 'Indian Ocean',   false, 1 FROM questions WHERE text = 'What is the largest ocean on Earth?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q7A3', id, 'Arctic Ocean',   false, 2 FROM questions WHERE text = 'What is the largest ocean on Earth?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q7A4', id, 'Pacific Ocean',  true,  3 FROM questions WHERE text = 'What is the largest ocean on Earth?' ON CONFLICT (id) DO NOTHING;

-- Q8: atomic number 1
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q8A1', id, 'Helium',   false, 0 FROM questions WHERE text = 'Which element has the atomic number 1?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q8A2', id, 'Hydrogen', true,  1 FROM questions WHERE text = 'Which element has the atomic number 1?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q8A3', id, 'Lithium',  false, 2 FROM questions WHERE text = 'Which element has the atomic number 1?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q8A4', id, 'Carbon',   false, 3 FROM questions WHERE text = 'Which element has the atomic number 1?' ON CONFLICT (id) DO NOTHING;

-- Q9: bones in human body
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q9A1', id, '196', false, 0 FROM questions WHERE text = 'How many bones are in the adult human body?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q9A2', id, '206', true,  1 FROM questions WHERE text = 'How many bones are in the adult human body?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q9A3', id, '216', false, 2 FROM questions WHERE text = 'How many bones are in the adult human body?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q9A4', id, '226', false, 3 FROM questions WHERE text = 'How many bones are in the adult human body?' ON CONFLICT (id) DO NOTHING;

-- Q10: square root of 144
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q10A1', id, '11', false, 0 FROM questions WHERE text = 'What is the square root of 144?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q10A2', id, '12', true,  1 FROM questions WHERE text = 'What is the square root of 144?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q10A3', id, '13', false, 2 FROM questions WHERE text = 'What is the square root of 144?' ON CONFLICT (id) DO NOTHING;
INSERT INTO answers (id, question_id, text, is_correct, position)
SELECT 'Q10A4', id, '14', false, 3 FROM questions WHERE text = 'What is the square root of 144?' ON CONFLICT (id) DO NOTHING;
