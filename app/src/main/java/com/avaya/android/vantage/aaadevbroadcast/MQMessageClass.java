package com.avaya.android.vantage.aaadevbroadcast;

public class MQMessageClass
{
    private String tipo;

    private String idioma;

    private String mensaje;

    public String getTipo ()
    {
        return tipo;
    }

    public void setTipo (String tipo)
    {
        this.tipo = tipo;
    }

    public String getIdioma ()
    {
        return idioma;
    }

    public void setIdioma (String idioma)
    {
        this.idioma = idioma;
    }

    public String getMensaje ()
    {
        return mensaje;
    }

    public void setMensaje (String mensaje)
    {
        this.mensaje = mensaje;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [tipo = "+tipo+", idioma = "+idioma+", mensaje = "+mensaje+"]";
    }
}
