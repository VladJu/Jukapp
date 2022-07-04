package com.example.jukapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    private FirebaseAuth mAuth;
    //общее хранилище данных
    private StorageReference reference;

    private RecyclerView recyclerViewMessages;
    private MessagesAdapter adapter;

    private EditText editTextMultiLineMessage;

    private SharedPreferences preferences;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itemSignOut) {
            mAuth.signOut();
            signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences= PreferenceManager.getDefaultSharedPreferences(this);
        db = FirebaseFirestore.getInstance();
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // создаем ссылку на хранилище
        //Ссылка на хранилище
        FirebaseStorage storage = FirebaseStorage.getInstance();
        //общее хранилище данных
        reference = storage.getReference();
        // Получаем досуп к главной папке , в которой все лежит, то есть в гланой папке,в которой лежат все файлы , мы создали папку для хранения изображений
        // reference = storageRef.child("images");
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMultiLineMessage = findViewById(R.id.editTextMultiLineMessage);
        ImageView imageViewSendMessage = findViewById(R.id.imageViewSendMessage);
        ImageView imageViewAddImage = findViewById(R.id.imageViewAddImage);
        adapter = new MessagesAdapter(this);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(adapter);
        imageViewAddImage.setOnClickListener(view -> {
            //получаем изображение с телефона
            //Получаем контент
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            //Указываем какой именно контент получаем
            intent.setType("image/jpeg");
            //получаем изображеие с локального хранилища (из галереи пользвоателя )
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            //запускаем активномть
            getImageActivityResultLauncher.launch(intent);
        });
        imageViewSendMessage.setOnClickListener(view -> sendMessage(editTextMultiLineMessage.getText().toString().trim(), null));
        if (mAuth.getCurrentUser() != null) {
            preferences.edit().putString("author",mAuth.getCurrentUser().getEmail()).apply();
        } else {
            Toast.makeText(this, "Not register", Toast.LENGTH_SHORT).show();
            signOut();
        }

    }

    private void sendMessage(String textOfMessage, String urlToImage) {
        // String textOfMessage = editTextMultiLineMessage.getText().toString().trim();
        Message message = null;
        String author=preferences.getString("author","Anonim");
        if (textOfMessage != null && !textOfMessage.isEmpty()) {
            message = new Message(author, textOfMessage, System.currentTimeMillis(), null);
        } else if (urlToImage != null && !urlToImage.isEmpty()) {
            message = new Message(author, null, System.currentTimeMillis(), urlToImage);
        }
        if (message != null) {
            db.collection("messages").add(message).addOnSuccessListener(documentReference -> {
                editTextMultiLineMessage.setText(" ");
                recyclerViewMessages.scrollToPosition(adapter.getItemCount() - 1);
            });
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        db.collection("messages").orderBy("date").addSnapshotListener((value, error) -> {
            if (value != null) {
                List<Message> messages = value.toObjects(Message.class);
                adapter.setMessages(messages);
                recyclerViewMessages.scrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    //регистрирует обратный вызов для контракта результата действия FirebaseUI:
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );

    // отправка фото
    private final ActivityResultLauncher<Intent> getImageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //получаем результат из когда , и он true
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //получаем адрес файла на телефоне
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            if (uri != null) {
                                StorageReference referenceToImage = reference.child("images/" + uri.getLastPathSegment());
                                referenceToImage.putFile(uri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                    @Override
                                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                        if (!task.isSuccessful()) {
                                            throw Objects.requireNonNull(task.getException());
                                        }
                                        // Ссылка на наше хранилище
                                        return referenceToImage.getDownloadUrl();
                                    }
                                }).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        //Получаем Адресс изображения на сервере и ЗАГРУЖАЕММ!
                                        Uri downloadUri = task.getResult();
                                        if (downloadUri != null) {
                                            sendMessage(null, downloadUri.toString());
                                        }
                                    }  // Handle failures
                                    // ...

                                });
                            }
                        }
                    }
                }
            });
    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Успешно выполнен вход в систему
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Toast.makeText(this, "Добро пожаловать :" + user.getEmail(), Toast.LENGTH_SHORT).show();
                preferences.edit().putString("author",user.getEmail()).apply();
            }
            // ...
        } else {
            if (response != null) {
                Toast.makeText(this, "Error :" + response.getError(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void signOut() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Выберите предпочтительные методы входа:
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.PhoneBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build());

// Создание и запуск намерения входа в систему
                Intent signInIntent = AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build();
                signInLauncher.launch(signInIntent);
            }
        });

    }
}