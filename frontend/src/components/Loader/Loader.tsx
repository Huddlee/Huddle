import React from 'react';
import './Loader.css';

const Loader: React.FC = () => {
  return (
    <div className="loader-wrapper">
      <div className="loader">
        <div className="circle">
          <div className="dot" />
          <div className="outline" />
        </div>
        <div className="circle">
          <div className="dot" />
          <div className="outline" />
        </div>
        <div className="circle">
          <div className="dot" />
          <div className="outline" />
        </div>
        <div className="circle">
          <div className="dot" />
          <div className="outline" />
        </div>
      </div>
    </div>
  );
};

export default Loader;
