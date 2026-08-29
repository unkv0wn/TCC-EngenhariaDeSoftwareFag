import { NextRequest, NextResponse } from "next/server";

const DataMock = {
    email: "teste@routewise.com",
    password: "12345678"
}

export async function POST(request: NextRequest) {
    const { email, password } = await request.json();
    
    if ( email !== DataMock.email || password !== DataMock.password) {
        return NextResponse.json(
            { message: "E-mail ou senha invalidos"},
            { status: 401 }
        );
    }

    const BackendResponse = await fetch(`${process.env.BACKEND_URL}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email })
    });

    if ( !BackendResponse.ok ) {
        return NextResponse.json(
            { message: "Não foi possivel gerar o token."},
            { status: 502}
        );
    }

    const { token } = await BackendResponse.json();

    const response = NextResponse.json({ email });

    response.cookies.set("routewise", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: "lax",
        path: "/",
        maxAge: 3600
    });

    return response;
}