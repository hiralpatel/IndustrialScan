package de.htwdd.industrialscan.model;

/**
 * A basic person model.
 */

public class Person
{
    String id;
    String firstName;
    String role;
    String lastName;
    String status;

    public Person(String id)
    {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGermanStatus()
    {
        if(status.equals("login"))
        {
            return "verfügbar";
        }
        else return "nicht verfügbar";
    }


    @Override
    public String toString() {
        return "Person{" +
                "id='" + id + '\'' +
                ", firstName='" + firstName + '\'' +
                ", role='" + role + '\'' +
                ", lastName='" + lastName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
