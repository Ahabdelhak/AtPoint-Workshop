package atpoint_workshop.com;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import atpoint_workshop.com.Common.Common;
import atpoint_workshop.com.Model.User;
import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    private Button btnSignin, btnRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference users;
    private FirebaseDatabase db;
    private RelativeLayout rootLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder().setDefaultFontPath("fonts/Arkhip_font.ttf").setFontAttrId(R.attr.fontPath).build());
        setContentView(R.layout.activity_main);

        //init View
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnSignin = (Button) findViewById(R.id.btnSignin);
        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

        //init Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference().child(Common.user_Mechanic_tbl);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRegisterDialouge();
            }
        });
        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSignInDialouge();
            }
        });
    }

    private void showSignInDialouge() {
        AlertDialog.Builder dialoge = new AlertDialog.Builder(this);
        dialoge.setTitle("SIGN IN ");
        dialoge.setMessage("Please use email to signin");
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View Register_Layout = layoutInflater.inflate(R.layout.layout_login, null);

        final MaterialEditText mEmail = Register_Layout.findViewById(R.id.editEmaill);
        final MaterialEditText mPassword = Register_Layout.findViewById(R.id.editPasswordl);

        dialoge.setView(Register_Layout);

        //set Button
        dialoge.setPositiveButton("SIGN IN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                //Set Disable Button If Processing
                btnSignin.setEnabled(false);
                if (TextUtils.isEmpty(mEmail.getText().toString())) {
                    Snackbar.make(rootLayout, "Please Enter Email Address", Snackbar.LENGTH_SHORT).show();
                }
                if (TextUtils.isEmpty(mPassword.getText().toString())) {
                    Snackbar.make(rootLayout, "Please Enter Password", Snackbar.LENGTH_SHORT).show();
                }
                if (mPassword.getText().toString().length() < 6) {
                    Snackbar.make(rootLayout, "Password Too Short", Snackbar.LENGTH_SHORT).show();
                }
                final SpotsDialog waitingdialog = new SpotsDialog(MainActivity.this);
                waitingdialog.show();
                mAuth.signInWithEmailAndPassword(mEmail.getText().toString(), mPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingdialog.dismiss();
                                startActivity(new Intent(MainActivity.this,Emergency.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        waitingdialog.dismiss();
                        Snackbar.make(rootLayout, "Failed To Login", Snackbar.LENGTH_SHORT).show();
                        btnSignin.setEnabled(true);
                    }
                });
            }
        });
        dialoge.setNegativeButton("CANCLE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialoge.show();
    }

    private void showRegisterDialouge() {
        AlertDialog.Builder dialoge = new AlertDialog.Builder(this);
        dialoge.setTitle("REGISTER ");
        dialoge.setMessage("Please use email to register");
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View Register_Layout = layoutInflater.inflate(R.layout.layout_register, null);

        final MaterialEditText mEmail = Register_Layout.findViewById(R.id.editEmail);
        final MaterialEditText mPassword = Register_Layout.findViewById(R.id.editPassword);
        final MaterialEditText mName = Register_Layout.findViewById(R.id.editName);
        final MaterialEditText mPhone = Register_Layout.findViewById(R.id.editPhone);

        dialoge.setView(Register_Layout);

        //set Button
        dialoge.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                if (TextUtils.isEmpty(mEmail.getText().toString())) {
                    Snackbar.make(rootLayout, "Please Enter Email Address", Snackbar.LENGTH_SHORT).show();
                }
                if (TextUtils.isEmpty(mPassword.getText().toString())) {
                    Snackbar.make(rootLayout, "Please Enter Password", Snackbar.LENGTH_SHORT).show();
                }
                if (mPassword.getText().toString().length() < 6) {
                    Snackbar.make(rootLayout, "Password Too Short", Snackbar.LENGTH_SHORT).show();
                }
                if (TextUtils.isEmpty(mName.getText().toString())) {
                    Snackbar.make(rootLayout, "Please Enter Name", Snackbar.LENGTH_SHORT).show();
                }
                if (TextUtils.isEmpty(mPhone.getText().toString())) {
                    Snackbar.make(rootLayout, "Please Enter Phone", Snackbar.LENGTH_SHORT).show();
                }

                //
                final SpotsDialog waitingdialog = new SpotsDialog(MainActivity.this);
                waitingdialog.show();
                //
                mAuth.createUserWithEmailAndPassword(mEmail.getText().toString(), mPassword.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){
                                    User user = new User();
                                    user.setEmail(mEmail.getText().toString());
                                    user.setPassword(mPassword.getText().toString());
                                    user.setName(mName.getText().toString());
                                    user.setPhone(mPhone.getText().toString());
                                    users.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    waitingdialog.dismiss();
                                                    startActivity(new Intent(MainActivity.this,Emergency.class));
                                                    finish();
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            waitingdialog.dismiss();
                                            Snackbar.make(rootLayout, "Registration Faild!", Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                                }else {
                                    waitingdialog.dismiss();
                                    Snackbar.make(rootLayout, "Registration Faild!", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        waitingdialog.dismiss();
                        Snackbar.make(rootLayout, "Registration Faild!", Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dialoge.setNegativeButton("CANCLE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialoge.show();
    }
    public void onStart() {
        super.onStart();
        FirebaseUser mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            // startActivity(new Intent(MainActivity.this,Welcome.class));
            //finish();
        } else {
        }
    }
}
