{
  "targets": [
    {
      "target_name": "jsthemis",
      "sources": [ "addon.cpp", "secure_message.cpp", "secure_keygen.cpp", "secure_session.cpp", "secure_cell_seal.cpp", "secure_cell_context_imprint.cpp", "secure_cell_token_protect.cpp" ],
      "libraries": ["/usr/lib/libthemis.a", "/usr/lib/libsoter.a"]
    }
  ]
}