import Register from './Register';
import Login from './Login';
import Profile from './Profile';
import { useState } from 'react';

function App() {
  const [user, setUser] = useState(null);

  return (
    <div>
      {!user ? (
        <>
          <Register />
          <Login setUser={setUser} />
        </>
      ) : (
        <Profile user={user} />
      )}
    </div>
  );
}

export default App;