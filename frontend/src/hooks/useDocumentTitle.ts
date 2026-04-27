import { useEffect } from 'react';

const useDocumentTitle = (title: string) => {
  useEffect(() => {
    const originalTitle = document.title;
    document.title = title;
    
    // Optional: Restore title on unmount
    return () => {
      document.title = originalTitle;
    };
  }, [title]);
};

export default useDocumentTitle;
