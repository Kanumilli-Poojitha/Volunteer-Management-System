import { useState } from "react";

function Register() {
  const [form, setForm] = useState({name:"",email:"",password:"",role:""});

  const handleSubmit = async () => {
    await fetch("http://localhost:8080/register",{
      method:"POST",
      headers:{"Content-Type":"application/json"},
      body: JSON.stringify(form)
    });
    alert("Registered Successfully");
  };

  return (
    <div>
      <h2>Register</h2>
      <input placeholder="Name" onChange={e=>setForm({...form,name:e.target.value})}/>
      <input placeholder="Email" onChange={e=>setForm({...form,email:e.target.value})}/>
      <input type="password" placeholder="Password" onChange={e=>setForm({...form,password:e.target.value})}/>
      <select onChange={e=>setForm({...form,role:e.target.value})}>
        <option>Select Role</option>
        <option>Volunteer</option>
        <option>Organizer</option>
      </select>
      <button onClick={handleSubmit}>Register</button>
    </div>
  );
}

export default Register;