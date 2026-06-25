import { useState } from "react";

function Login({setUser}) {
  const [form, setForm] = useState({email:"",password:""});

  const handleLogin = async () => {
    const res = await fetch("http://localhost:8080/login",{
      method:"POST",
      headers:{"Content-Type":"application/json"},
      body: JSON.stringify(form)
    });
    const data = await res.json();
    setUser(data);
  };

  return (
    <div>
      <h2>Login</h2>
      <input placeholder="Email" onChange={e=>setForm({...form,email:e.target.value})}/>
      <input type="password" placeholder="Password" onChange={e=>setForm({...form,password:e.target.value})}/>
      <button onClick={handleLogin}>Login</button>
    </div>
  );
}

export default Login;