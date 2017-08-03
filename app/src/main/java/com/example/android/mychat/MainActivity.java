package com.example.android.mychat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final String ANONYMOUS = "Anonymous";
    private static int SIGN_IN_REQUEST_CODE = 1;
    private static final int RC_PHOTO_PICKER =  2;
    private static final int RC_GALLERY_IMAGE =  3;
    private static final int RC_CAMERA_IMAGE =  4;
    private static final int RC_GALLERY_PROFILE_IMAGE =  5;
    private static final int RC_CAMERA_PROFILE_IMAGE =  6;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private FirebaseListAdapter<ChatMessage> adapter;
    RelativeLayout activity_main;
    FloatingActionButton fab;

    ListView listOfMessage;
    HashMap<String,Integer> userNames;
    HashMap<String,String> userPhotos;


    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference rootDatabaseReference;     // path: /
    private DatabaseReference convoDatabaseReference;    // path: /Conversations/
    private DatabaseReference databaseReference;         // path: /Conversations/Global
    private DatabaseReference privateRoomsReference;     // path: /PrivateRooms/
    private DatabaseReference userListReference;         // path: /Users
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private FirebaseUser user;
    private StorageReference mChatPhotosStorageRefrence;
    private StorageReference mChatProfilePicturesStorageRefrence;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private DatabaseReference notifications;

     ImageButton mPhotoPickerButton;
    EditText input;

    private boolean notificationArrived = false ;
    private boolean isInPrivateChat = false;
    private String privateChatFriend="";

    String mUsername;
    Random rand = new Random();
    private String m_Text = "";

    private ChildEventListener mChildEventListener;


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_sign_out) {
            AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Snackbar.make(activity_main, R.string.SignedOut,Snackbar.LENGTH_SHORT).show();
                    finish();
                }
            });
        }else if(item.getItemId() == R.id.joinRoom){
            joinRoom();
        }else if(item.getItemId() == R.id.setProfilePic){
            chooseProfilePhoto();
        }else if(item.getItemId() == R.id.globalRoomMenuItem){
            enterRootConversation();
        }else if(item.getItemId() == R.id.chooseOpenRoom){
            chooseOpenRoom();
        }
        return true;
    }

    private void joinRoom(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.joinRoom);

        // Set up the input
        final EditText inputRoomName = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        inputRoomName.setInputType(InputType.TYPE_CLASS_TEXT );
        builder.setView(inputRoomName);

        // Set up the buttons
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = inputRoomName.getText().toString().trim();

                //Open a public Room
                openRoom(m_Text);
            }
        });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void enterRootConversation(){
        databaseReference = convoDatabaseReference.child(getString(R.string.Global_room_chat));
        setTitle(getString(R.string.app_name));
        updateScreen();
        isInPrivateChat = false;
        privateChatFriend ="";
    }
    private void openRoom(String roomName){
        if(!roomName.isEmpty()){
            databaseReference = convoDatabaseReference.child(roomName);
            setTitle(roomName);
            updateScreen();
        }else{
            enterRootConversation();
        }
        isInPrivateChat = false;
        privateChatFriend ="";
    }

    private void openPrivateRoom(String friend){
        String roomCode = mUsername.hashCode() + friend.hashCode() + "";
        databaseReference = privateRoomsReference.child(roomCode);
        setTitle(friend);
        updateScreen();
        isInPrivateChat = true;
        privateChatFriend = friend;
    }

    private void updateScreen(){
        //update View
        reattachDBReadListener();
        //Load content
        displayChatMessage();
        scrollMyListViewToBottom();
    }
    private void reattachDBReadListener(){
        detachDatabaseReadListener();
        attachDatabaseReadListener();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SIGN_IN_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                Snackbar.make(activity_main, R.string.SIGNED_IN,Snackbar.LENGTH_SHORT).show();
                displayChatMessage();
            }else if(resultCode == RESULT_CANCELED){
                    Snackbar.make(activity_main, R.string.CANT_SIGN_IN,Snackbar.LENGTH_SHORT).show();
                    finish();
            }
        }else if(requestCode == RC_PHOTO_PICKER || requestCode == RC_GALLERY_IMAGE){
            if(resultCode == RESULT_OK ){
                Uri selectedImageUri = data.getData();
                uploadPictureToFirebase(selectedImageUri);
//            }else if (resultCode == RESULT_CANCELED){

            }
        }else if (requestCode == RC_CAMERA_IMAGE && resultCode == RESULT_OK) {
            Bitmap bitmap= (Bitmap)data.getExtras().get(getString(R.string.data));

            Uri selectedImageUri = getImageUri(bitmap);
            uploadPictureToFirebase(selectedImageUri);

        }else if (requestCode == RC_GALLERY_PROFILE_IMAGE && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();

            uploadProfilePictureToFirebase(selectedImageUri);

        }else if (requestCode == RC_CAMERA_PROFILE_IMAGE && resultCode == RESULT_OK) {
            Bitmap bitmap= (Bitmap)data.getExtras().get(getString(R.string.data));

            Uri selectedImageUri = getImageUri(bitmap);
            uploadProfilePictureToFirebase(selectedImageUri);
        }
    }


    public void uploadPictureToFirebase(Uri imageUri){
        StorageReference photoRef = mChatPhotosStorageRefrence.child(imageUri.getLastPathSegment().replace(":","")+"_user"+mUsername+"_"+rand.nextInt(420)+".jpg");
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setCancelable(false);

        //Upload File to Firebase Storage
        photoRef.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @SuppressWarnings("VisibleForTests")
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                ChatMessage chatPictureMessage = new ChatMessage(null,mUsername,downloadUrl.toString());
                databaseReference.push().setValue(chatPictureMessage);
                dialog.dismiss();
//                scrollMyListViewToBottom();
                updateScreen();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @SuppressWarnings("VisibleForTests")
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = 100* (taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());

                dialog.setMessage(getString(R.string.Uploading)+progress+getString(R.string.percentage));
                dialog.show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e){
                //if the upload is not successfull
                //hiding the progress dialog
                dialog.dismiss();

                //and displaying error message
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    public void uploadProfilePictureToFirebase(Uri imageUri){
        StorageReference photoRef = mChatProfilePicturesStorageRefrence.child(mUsername+".jpg");
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setCancelable(false);

        //Upload profile picture File to Firebase Storage
        photoRef.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @SuppressWarnings("VisibleForTests")
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                userListReference.child(mUsername).child(getString(R.string.profilePhotoUrl)).setValue(downloadUrl.toString());
                dialog.dismiss();
                updateScreen();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @SuppressWarnings("VisibleForTests")
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = 100* (taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());

                dialog.setMessage(getString(R.string.Uploading)+progress+getString(R.string.percentage));
                dialog.show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e){
                //if the upload is not successfull
                //hiding the progress dialog
                dialog.dismiss();

                //and displaying error message
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    public Uri getImageUri(Bitmap mBitmap) {
        Uri uri = null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            // Calculate inSampleSize
            options.inSampleSize = 1;

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            Bitmap newBitmap = Bitmap.createScaledBitmap(mBitmap, 200, 200,
                    true);
            File file = new File(this.getFilesDir(), getString(R.string.image)
                    + new Random().nextInt() + getString(R.string.photoExtension));
            FileOutputStream out = this.openFileOutput(file.getName(),
                    Context.MODE_PRIVATE);
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            //get absolute path
            String realPath = file.getAbsolutePath();
            File f = new File(realPath);
            uri = Uri.fromFile(f);

        } catch (Exception e) {
            Log.e(getString(R.string.Error_saving_selfie), e.getMessage());
        }
        return uri;
    }

    private void chooseOpenRoom(){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
        builderSingle.setIcon(R.mipmap.ic_seekret);
        builderSingle.setTitle(R.string.chatrooms);



        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.select_dialog_singlechoice);

        convoDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    arrayAdapter.add(child.getKey());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        builderSingle.setNegativeButton(R.string.CANCEL, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String roomName = arrayAdapter.getItem(which);
                if(roomName != "Global")
                    openRoom(roomName);
                else
                    enterRootConversation();
            }
        });
        builderSingle.show();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;
        //HIDE KEYBOARD
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        //Initialize Firebase components
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootDatabaseReference =firebaseDatabase.getReference();
        convoDatabaseReference = rootDatabaseReference.child(getString(R.string.conversationsRef));
        databaseReference = convoDatabaseReference.child(getString(R.string.GlobalConvRef));
        privateRoomsReference = rootDatabaseReference.child(getString(R.string.PrivateConvRef));
        userListReference = rootDatabaseReference.child(getString(R.string.UserListRef));
        firebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mChatPhotosStorageRefrence = mFirebaseStorage.getReference().child(getString(R.string.chatPhotosRef));
        mChatProfilePicturesStorageRefrence = mFirebaseStorage.getReference().child(getString(R.string.usersProfilePicsRef));
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        userNames = new HashMap<>();
//        userPhotos = new HashMap<>();

        //Initialize references to views
        activity_main = (RelativeLayout) findViewById(R.id.activity_main);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        input = (EditText) findViewById(R.id.input);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = input.getText().toString();
                if(message.trim().length() > 0){
                    databaseReference.push().setValue(new ChatMessage(message,mUsername));
                    input.setText("");
                    updateScreen();
                    if(!getTitle().equals(R.string.app_name) && isInPrivateChat){
                        sendNotificationToUser(privateChatFriend,": "+message);

                    }
                }
                scrollMyListViewToBottom();
            }
        });

        // Enable Send button when there's text to send
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                scrollMyListViewToBottom();
                if (charSequence.toString().trim().length() > 0) {
                    fab.setEnabled(true);
                    fab.setColorFilter(Color.rgb(255,255,255));
                } else {
                    fab.setEnabled(false);
                    fab.setColorFilter(Color.rgb(169,169,169));
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Send ad message photo, and NOT profile photo
                uploadPicture(false);
            }
        });


        attachDatabaseReadListener();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if(user == null){
                    //Signed Out
                    onSignedOutCleanup();
                    startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                            .setProviders(
                                    AuthUI.GOOGLE_PROVIDER,
                                    AuthUI.FACEBOOK_PROVIDER,
                                    AuthUI.EMAIL_PROVIDER
                            ).setTheme(R.style.Theme_AppCompat_DayNight).setLogo(R.mipmap.ic_seekret)
                            .build(),SIGN_IN_REQUEST_CODE);

                }else{
                    //Signed In
                    onSignedInInitialize(user);


//                    if(user.getPhotoUrl() != null)
//                        uploadProfilePictureToFirebase(user.getPhotoUrl());

                    //Check if user exist in database
                    userListReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if(mUsername!=ANONYMOUS){
                                updateScreen();
                                if (!snapshot.child(mUsername).exists()) {
                                    ChatUser chatUser = new ChatUser(user);
                                    userListReference.child(chatUser.getUserName()).setValue(chatUser);
                                    chooseProfilePhoto();
                                }else{
                                    //check if user have profile picture
                                    if(!snapshot.child(mUsername).child(getString(R.string.profilePhotoUrl)).exists()){
                                        chooseProfilePhoto();
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });


                    if(getIntent().getExtras()!=null){
                        String username = getIntent().getExtras().getString(getString(R.string.title));
                        int reqCode = getIntent().getExtras().getInt(getString(R.string.reqCode));
                        if(reqCode == 420 && user!=null && mUsername !=ANONYMOUS){
                            openPrivateRoom(username);

                        }
                    }
                    //Load content
                    displayChatMessage();


                    checkForNotifications();

                    reattachDBReadListener();

                    scrollMyListViewToBottom();
                }
            }
        };
        loadFirebaseRemoteConfigSettings();

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(user!=null){
                    if(notificationArrived)
                        notificationArrived=false;
                    attachDatabaseReadListener();
                }

            }
        }, 1000, 6500);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(user!=null){
//                    displayChatMessage();
                    reattachDBReadListener();
                }

            }
        }, 3500, 5000);



    }
    private void checkForNotifications(){
        notifications = userListReference.child(mUsername).child(getString(R.string.notificsRequests));
        notifications.addValueEventListener(checkDBforNotificationsListener);
    }

    private ValueEventListener checkDBforNotificationsListener = new ValueEventListener(){
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.exists()){
//                    Map notification = dataSnapshot.getValue(Map.class);

                String username = (String) dataSnapshot.child(getString(R.string.username)).getValue();
                String message = (String) dataSnapshot.child(getString(R.string.message)).getValue();

                if(!notificationArrived && privateChatFriend!=username){
                    showNotification(username,message);
                    newMessagePopUp(username,message);
                    notificationArrived=true;
                }

                userListReference.child(mUsername).child(getString(R.string.notificsRequests)).removeValue();
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    private void uploadPicture(boolean profilePicture){
        final int gallery;
        final int camera;

        if(profilePicture){
            gallery = RC_GALLERY_PROFILE_IMAGE;
            camera = RC_CAMERA_PROFILE_IMAGE;
        }else{
            gallery = RC_GALLERY_IMAGE;
            camera = RC_CAMERA_IMAGE;
        }

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
        builderSingle.setTitle("Choose An Option");


        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("Gallery");
        arrayAdapter.add("Camera");

        builderSingle.setNegativeButton(
                "cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(
                arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(pickPhoto, gallery);
                                break;

                            case 1:
                                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                startActivityForResult(takePicture, camera);
                                break;
                        }

                    }
                });
        builderSingle.show();
    }
    private void loadFirebaseRemoteConfigSettings(){
        //Firebase Remote Config settings
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG).build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(getString(R.string.max_msg_length),DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
        fetchConfig();
    }

    private void chooseProfilePhoto(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater factory = LayoutInflater.from(MainActivity.this);

        final View view = factory.inflate(R.layout.dialog_uploadprofilephoto, null);
        builder.setView(view);
        // Add the buttons
        builder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button

            }
        });
        builder.setNegativeButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog

            }
        });


        ImageView chosenPicture = (ImageView) view.findViewById(R.id.chosen_picture);

        chosenPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Upload as Profile Picture
                uploadPicture(true);
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void displayChatMessage() {
        listOfMessage = (ListView) findViewById(R.id.list_of_message);
        adapter = new FirebaseListAdapter<ChatMessage>(this,ChatMessage.class,R.layout.list_item,databaseReference) {
            @Override
            protected void populateView(View v, ChatMessage model, int position) {
                //Check if this model is not recycled
                if(model != null){
                    final TextView messageText,messageUser,messageTime;
                    //Get refrences to the views of list_item.xml
                    messageText= (TextView) v.findViewById(R.id.message_text);
                    messageUser= (TextView) v.findViewById(R.id.message_user);
                    messageTime= (TextView) v.findViewById(R.id.message_time);
                    final ImageView photoImageView = (ImageView) v.findViewById(R.id.imageUser);
                    final ImageView picture_user = (ImageView) v.findViewById(R.id.picture_user);
                    final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.indeterminateBar);

                    final String friendsName = model.getMessageUser();

                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //if User is not trying to open a Chat with himself , and if User is not already in Private room with friendsName
                            if(!mUsername.equals(friendsName) && getTitle()!=friendsName) {
                                openPrivateRoom(friendsName);
                                sendNotificationToUser(friendsName,getString(R.string.userOpenedAchatwithU));

                            }
                        }
                    });

                    //Colorized Usernames
                    if(!userNames.containsKey(model.getMessageUser())){
                        int randColor = Color.rgb(rand.nextInt(255),rand.nextInt(255),rand.nextInt(255));
                        userNames.put(model.getMessageUser(),randColor);
                        messageUser.setTextColor(randColor);


                    }else{
                        messageUser.setTextColor(userNames.get(model.getMessageUser()));
                    }

                    //load Users Profile Images
                    userListReference.child(model.getMessageUser()).child(getString(R.string.profilePhotoUrl)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final String photoUrl = dataSnapshot.getValue(String.class);
                            if(photoUrl!=null){
                                Glide.with(photoImageView.getContext())
                                        .load(photoUrl)
                                        .fitCenter()
                                        .into(picture_user);
                                picture_user.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        openFullscreenImage(picture_user.getContext(),photoUrl);
                                    }
                                });
                            }
                            //                        }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    //load User sent picture, if exists
                    final String photoUrl = model.getPhotoUrl();
                    boolean isPhoto = photoUrl !=null;
                    if(isPhoto){
                        //Photo Message
                        progressBar.setVisibility(View.VISIBLE);
                        Glide.with(photoImageView.getContext())
                                .load(photoUrl).listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .centerCrop()
                        .override(123, 123)
                        .error(R.drawable.com_facebook_button_icon)
                        .into(photoImageView);

                        photoImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                openFullscreenImage(photoImageView.getContext(),photoUrl);
                            }
                        });
                    }else{
                        //Message Text
                        messageText.setText(model.getMessageText());
                        photoImageView.setVisibility(View.GONE);

                    }
                    //set rest of message details
                    messageUser.setText(model.getMessageUser());
                    messageTime.setText(DateFormat.format("dd/MM HH:mm",model.getMessageTime()));

                }else{
                    adapter.cleanup();
                }
            }
        };
        listOfMessage.setAdapter(adapter);
//        checkForNotifications();
        scrollMyListViewToBottom();
    }

    private void openFullscreenImage(Context context , String photoUrl){
        final Dialog nagDialog = new Dialog(MainActivity.this,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nagDialog.setCancelable(false);
        nagDialog.setContentView(R.layout.preview_image);
        Button btnClose = (Button)nagDialog.findViewById(R.id.btnIvClose);
        ImageView ivPreview = (ImageView)nagDialog.findViewById(R.id.iv_preview_image);
        //                            Drawable picture = photoImageView.getDrawable();
        //                            ivPreview.setBackgroundDrawable(photoImageView.getDrawable());
        Glide.with(context)
                .load(photoUrl)
                .fitCenter()
                .into(ivPreview);
        ivPreview.setBackgroundColor(Color.rgb(0,0,0));



        View.OnClickListener closeImageDialog = new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                nagDialog.dismiss();
            }


        };
        btnClose.setOnClickListener(closeImageDialog);
        ivPreview.setOnClickListener(closeImageDialog);
        nagDialog.show();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(getIntent().getExtras()!=null){
            String username = getIntent().getExtras().getString(getString(R.string.title));
            int reqCode = getIntent().getExtras().getInt(getString(R.string.reqCode));
            if(reqCode == 420)
                openPrivateRoom(username);
        }

    }
    private void showNotification(String title, String body){
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Intent intent = new Intent(this,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(getString(R.string.title), title);
        intent.putExtra(getString(R.string.reqCode), 420);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_ONE_SHOT);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_send)
                .setContentTitle(title)
                .setContentText(body.replace(":",""))
                .setAutoCancel(true)
                .setSound(soundUri)
                .setSmallIcon(R.mipmap.ic_seekret)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0,notificationBuilder.build());

    }
    private void newMessagePopUp(final String userName,String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(userName + text);
        builder.setCancelable(true);

        builder.setPositiveButton(
               "Yes",
               new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       openPrivateRoom(userName);
                       dialog.cancel();
                   }
               });

        builder.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = builder.create();
        if(privateChatFriend != userName)
            alert.show();

    }
    public void sendNotificationToUser(String user, final String message) {
        DatabaseReference notifications = userListReference.child(user).child(getString(R.string.notificsRequests));

        Map notification = new HashMap<>();
        notification.put(getString(R.string.username), mUsername);
        notification.put(getString(R.string.message), message);

        notifications.setValue(notification);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
    private void scrollMyListViewToBottom() {
        listOfMessage.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                listOfMessage.setSelection(adapter.getCount() - 1);
            }
        });
    }
    private void onSignedInInitialize(FirebaseUser username){
        if(username.getDisplayName() != null){
            mUsername = username.getDisplayName();
        }else if(username.getEmail()!= null){
            mUsername = username.getEmail();
        }else{
            mUsername = ANONYMOUS+" "+(1+rand.nextInt(8))+rand.nextInt(421);
        }
        attachDatabaseReadListener();
        Snackbar.make(activity_main,"Welcome "+mUsername,Snackbar.LENGTH_SHORT).show();

    }
    private void onSignedOutCleanup(){
        mUsername = ANONYMOUS;
//        adapter.cleanup();
//        mMessageAdapter.clear();
        detachDatabaseReadListener();

    }
    private void attachDatabaseReadListener(){
        if(mChildEventListener == null){
            mChildEventListener= new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    scrollMyListViewToBottom();
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
//                    scrollMyListViewToBottom();
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };

            databaseReference.addChildEventListener(mChildEventListener);
        }

    }

    private void detachDatabaseReadListener() {
        if(mChildEventListener != null){
            databaseReference.removeEventListener(mChildEventListener);
            mChildEventListener=null;
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mAuthStateListener !=null) {
            firebaseAuth.addAuthStateListener(mAuthStateListener);
        }
        attachDatabaseReadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthStateListener !=null){
            firebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
    }

    public void fetchConfig(){
        long cacheExpiration = 3600;

        if(mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrivedLengthLimit();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("TAG","Error Fetching Config from firebase"+e.getMessage());
                        applyRetrivedLengthLimit();

            }
        });

    }

    private void applyRetrivedLengthLimit(){
        Long new_msg_length = mFirebaseRemoteConfig.getLong(String.valueOf(R.string.max_msg_length));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        Log.d("TAG",String.valueOf(R.string.max_msg_length)+" = " + DEFAULT_MSG_LENGTH_LIMIT);

    }
}
