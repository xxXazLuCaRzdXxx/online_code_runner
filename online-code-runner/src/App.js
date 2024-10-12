import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useSpring, animated } from 'react-spring';
import './App.css';

function App() {
  const [code, setCode] = useState('');
  const [language, setLanguage] = useState('python');
  const [input, setInput] = useState('');
  const [output, setOutput] = useState('');
  const [exec, setExec] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [animationCompleted, setAnimationCompleted] = useState(false); // New state to track animation completion

  // Animation for "Online Code Runner" text
  const [text, setText] = useState('');
  useEffect(() => {
    const textToType = 'Online Code Runner';
    let currentIndex = 0;
    const interval = setInterval(() => {
      if (currentIndex <= textToType.length) {
        setText(textToType.substring(0, currentIndex));
        currentIndex++;
      } else {
        clearInterval(interval);
        setAnimationCompleted(true);
      }
    }, 60);
    return () => clearInterval(interval);
  }, []);

  // Cursor animation
  const cursorSpring = useSpring({
    from: { opacity: 0 },
    to: { opacity: 1 },
    loop: { reverse: true },
    delay: 50, // Delay after the text is fully written
  });

  // Button animation
  const buttonSpring = useSpring({
    from: { opacity: 0, transform: 'translateY(100%)' },
    to: async (next) => {
      await next({ opacity: 1, transform: 'translateY(0)' });
    },
    delay: 1500, // Delay after the typing animation completes
  });

  const handleCodeChange = (e) => {
    setCode(e.target.value);
  };

  const handleLanguageChange = (e) => {
    setLanguage(e.target.value);
  };

  const handleInputChange = (e) => {
    setInput(e.target.value);
  };

  const handleRunCode = async () => {
    setLoading(true);
    setError('');
    setOutput('');

    try {
      const response = await axios.post('http://localhost:5000/run', {
        code,
        language,
        input,
      });
      setOutput(response.data.output);
      setExec(response.data.executionTime);
    } catch (err) {
      setError(err.response ? err.response.data.error : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="App">
      <h1>
        <animated.span>{text}</animated.span>
        <animated.span style={cursorSpring}>|</animated.span>
      </h1>
      {animationCompleted && ( // Only render buttons after animation completion
        <>
          <animated.textarea
            value={code}
            onChange={handleCodeChange}
            placeholder="Write your code here..."
            style={buttonSpring}
          ></animated.textarea>
          <animated.select
            value={language}
            onChange={handleLanguageChange}
            style={buttonSpring}
          >
            <option value="python">Python</option>
            <option value="javascript">JavaScript</option>
            <option value="ruby">Ruby</option>
          </animated.select>
          <animated.textarea
            value={input}
            onChange={handleInputChange}
            placeholder="Input for your code..."
            style={buttonSpring}
          ></animated.textarea>
          <animated.button
            onClick={handleRunCode}
            disabled={loading}
            style={buttonSpring}
          >
            {loading ? 'Running...' : 'Run Code'}
          </animated.button>
          {error && <div className="error">{error}</div>}
          <animated.pre className="output" style={buttonSpring}>
            {output}
            {exec}
          </animated.pre>
        </>
      )}
    </div>
  );
}

export default App;