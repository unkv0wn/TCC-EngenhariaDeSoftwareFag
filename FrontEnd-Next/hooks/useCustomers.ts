"use client";

import { useCallback, useState } from "react";

import type { CustomerFormData } from "@/lib/validations/customer";

export interface Customer extends CustomerFormData {
  id: string;
}

const INITIAL_CUSTOMERS: Customer[] = [
  {
    id: "1",
    personType: "fisica",
    document: "529.982.247-25",
    name: "Maria Souza",
    type: "cliente",
    email: "maria.souza@email.com",
    phone: "(11) 98765-4321",
    address: {
      zipCode: "01310-100",
      street: "Avenida Paulista",
      number: "1000",
      district: "Bela Vista",
      city: "São Paulo",
      state: "SP",
    },
  },
  {
    id: "2",
    personType: "juridica",
    document: "11.222.333/0001-81",
    name: "Distribuidora ABC Ltda",
    tradeName: "ABC Bebidas",
    type: "cliente",
    email: "contato@abcbebidas.com.br",
    phone: "(41) 3025-4477",
    address: {
      zipCode: "80010-000",
      street: "Rua XV de Novembro",
      number: "500",
      district: "Centro",
      city: "Curitiba",
      state: "PR",
    },
  },
  {
    id: "3",
    personType: "fisica",
    document: "123.456.789-09",
    name: "João Pereira",
    type: "cliente",
    email: "joao.pereira@email.com",
    phone: "(31) 99876-5432",
    address: {
      zipCode: "30130-010",
      street: "Avenida Afonso Pena",
      number: "200",
      complement: "Apto 302",
      district: "Centro",
      city: "Belo Horizonte",
      state: "MG",
    },
  },
  {
    id: "4",
    personType: "juridica",
    document: "12.345.678/0001-95",
    name: "Fornecedora Central de Alimentos Ltda",
    tradeName: "Central Alimentos",
    type: "fornecedor",
    email: "vendas@centralalimentos.com.br",
    phone: "(51) 3212-8899",
    address: {
      zipCode: "90010-150",
      street: "Rua dos Andradas",
      number: "1200",
      district: "Centro Histórico",
      city: "Porto Alegre",
      state: "RS",
    },
  },
  {
    id: "5",
    personType: "juridica",
    document: "98.765.432/0001-98",
    name: "Comércio e Distribuição Real Ltda",
    tradeName: "Real Distribuidora",
    type: "ambos",
    email: "contato@realdistribuidora.com.br",
    phone: "(21) 2536-7788",
    address: {
      zipCode: "20040-020",
      street: "Rua da Assembleia",
      number: "77",
      district: "Centro",
      city: "Rio de Janeiro",
      state: "RJ",
    },
  },
  {
    id: "6",
    personType: "fisica",
    document: "987.654.321-00",
    name: "Ana Lima",
    type: "cliente",
    email: "ana.lima@email.com",
    phone: "(48) 99654-1122",
    address: {
      zipCode: "88010-400",
      street: "Rua Felipe Schmidt",
      number: "50",
      district: "Centro",
      city: "Florianópolis",
      state: "SC",
    },
  },
];

export function useCustomers() {
  const [customers, setCustomers] = useState<Customer[]>(INITIAL_CUSTOMERS);

  const createCustomer = useCallback((data: CustomerFormData) => {
    setCustomers((prev) => [...prev, { ...data, id: crypto.randomUUID() }]);
  }, []);

  const updateCustomer = useCallback((id: string, data: CustomerFormData) => {
    setCustomers((prev) => prev.map((customer) => (customer.id === id ? { ...data, id } : customer)));
  }, []);

  const deleteCustomer = useCallback((id: string) => {
    setCustomers((prev) => prev.filter((customer) => customer.id !== id));
  }, []);

  return { customers, createCustomer, updateCustomer, deleteCustomer };
}
